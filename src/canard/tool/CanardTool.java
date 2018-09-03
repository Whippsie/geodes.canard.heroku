package canard.tool;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import canard.Block;
import canard.CanardFactory;
import canard.CanardModel;
import canard.Configuration;
import canard.Flag;
import canard.Rel;
import canard.Topic;
public class CanardTool{
	public static final String INPUTLAUNCH = "input/master.launch";
	public static String OUTPUTFILE =  "";
	public static String UILAUNCH = "";
	public static int uniqueID = 0; // Generates unique link ID

	public static void main(String[] args) throws IOException {
		UILAUNCH = args[0]; // relative path to the Config file in the webserver
		OUTPUTFILE = "public/files/gmfXMI.canard";
		CanardHelper.initModel();
		String text = FileReaderTool.readFile(INPUTLAUNCH);
		parseLaunch(text,null);
		makeXMI();
	}

	
	/* Takes a .launch (or part of .launch) and execute the includes, remap.
	 * Also checks the group if conditions according to a configuration given */
	private static void parseLaunch(String text, Configuration config){
		// We get the first position of every element
		int posIncl = text.indexOf("<include");
		int posGroup = text.indexOf("<group");
		int posRemap = text.indexOf("<remap");
		int posArg = text.indexOf("<arg");
		
		// Variables used to keep count of our position in the match loop
		int cptGroup = 0 ; // +1 if new group is found, -1 if a group is closed. 0 means we went through the main group
		int totalGroups = 0 ; // Count number of recurcively group found in a main group
		int debutGroupe = -1 ; // Remembers the beginning position of the outer group
		
		// Get the smallest element found between include, group and remap
		int firstMin = minimum (posIncl, posGroup, posRemap);
		
		config = makeConfig(config, posArg, firstMin, text);
		
		beforeGroup(firstMin, posGroup, text);
		
		// We will match all the 'group' keyword in the text given in parameter
		String wordToFind = "group";
		Pattern word = Pattern.compile(wordToFind);
		Matcher match = word.matcher(text);
		
		// Loops through all groups found in the text given
		while (match.find()) {
			
			// First time, we match the initial group's beginning
			if (debutGroupe == -1){
				debutGroupe = match.start()-1;
			}
			
			// We open a new group
			if (text.charAt(match.start()-1) == '<'){
				cptGroup ++;
				totalGroups ++;
				
			// We close a group
			}else{
				cptGroup --;
			}
			
			// If we have walked through the main group completely
			if (cptGroup == 0){
				
				// We cut the text to get the data within the beginning and end of the main group
				int finGroupe = match.end()+1;
				String groupText = text.substring(debutGroupe,finGroupe);

				// Gets the full name of the argument
				int posiArg = groupText.indexOf("arg");
				posiArg += 4; // $(arg camera)">, always add 4 char space
				int posEndArg = groupText.indexOf(">");
				String arg = groupText.substring(posiArg,posEndArg-2);

				// Checks if the argument exists
				Flag f = FlagsBase.getFlagByName(arg);
				
				// Checks it's value in the configuration given
				EMap<Flag,String> mapping = config.getConfigflags();
				
				// If, in this configuration, the argument is true, we execute the group if
				String test = mapping.get(f);
				if (mapping.get(f).equals("true")){
					
					// Split the text to only get the inside of the outer gruop if
					String subgroupText = text.substring(debutGroupe+posEndArg,finGroupe-8);
					
					// If we found no groups initially, we already executed the nodes and remap
					if (totalGroups == 0){
						return;
					}else{
						// We call recursively to execute the text within the valid group if
						parseLaunch(subgroupText, config);
						totalGroups = 0;
						debutGroupe = -1;
					}
				}else{
					// Argument is false, we reset the variable's value and go to next match
					debutGroupe = -1;
				}
			}
		}
	}
	
	
	
	/* Handles the argument section - flags and features
	* Config is null if this is first time we parse
	* Thus, we are trying to parse the whole master.launch file */
	private static Configuration makeConfig(Configuration config, int posArg, int firstMin, String text) {
		if (config == null){
			
			// We fetch only the arguments and ignore the rest for now
			String argText = text.substring(posArg,firstMin-1);
			
			// We generate a configuration based on these arguments
			config = GenConfig.genConfigText(argText);
			
			// We modify the configuration just created according to the user's config file on the web server
			GenConfig.adaptConfig(canard.tool.FileReaderTool.readFile(UILAUNCH), config);
		}
		return config;
	}
	
	
	/* This function handles the execution of an include or a remap line
	 * If a group is found later, the execution of the remaining code will be done recursively
	 * In the parseLaunch function */
	private static void beforeGroup(int firstMin, int posGroup, String text) {
		// If we have an include/remap before a group or no group at all
		if (firstMin < posGroup || posGroup < 0){
			String tempText = text;
			
			// If a group is present, but after an include/remap
			if (posGroup >= 0){
				// We want to only work on the include/remap, for now
				tempText = text.substring(0,posGroup);
			}
			
			// We execute the subtext obtained
			// If the line was a remap, the following line does nothing
			exeInclude(tempText);
			// If the line was an include, the following line does nothing
			relationsFromLaunch(tempText);
		}
	}
	
	
	/* Find all the includes in the text passed as parameter
	 * Creates the EObject and add them to the model */
	static ArrayList<String> nodes = new ArrayList<String>();
	private static void exeInclude(String text){

		// Excludes the ending bracket </include>
		String wordToFind = "<include";
		Pattern word = Pattern.compile(wordToFind);
		Matcher match = word.matcher(text);
		

		while (match.find()) {
			 // Finds the end of the line
		     int endInclude = text.indexOf('>',match.end());
		     
		     // Makes sure the line isn't a comment, if so, ignores it
		     if (!StringUtils.checkComment(text,match.start())){
		    	 
		    	 // Manages 1 line at a time
			     String temp = text.substring(match.start()+1, endInclude);
			     
			     // Reverse search from the end to get the node's name
			     int nodePos = temp.lastIndexOf("/");
			     if (nodePos == temp.length()-1){
			    	 //We have a / at the end, probably the machine file
			    	 continue;
			     }
			     
			     // Gets the node name using the position found
			     String node = temp.substring(nodePos+1,(endInclude-1)-match.start()-1);
			     
			     // Remove the inadequat information to only get the name
			     node = node.replace(".launch", "");
			     node = node.replace("\"", "");
			     
			     // If we have no EObject related to that name
			     if (CanardHelper.getBlockFromName(node)==null){
			    	 
			    	 // Create one and add it to the existant model
			    	 CanardHelper.model.getBlocks().add(makeBlock(node));
			     }
				 
		     }
		}
	}

	
	//0 topic, 1 bad topic
	private static Topic linkTopicToBlock(String line, int topicType){
		String[] lineSplit = line.split("/");
	    Block currBlock = CanardHelper.getBlockFromName(lineSplit[0]);
	     
	     //Pour gerer le cas ou un noeud pas include, mais remap
	     if (currBlock == null && lineSplit[0].contains("node")){
	    	 currBlock = makeBlock(lineSplit[0]);
	     }
	     
	     Topic currTopic = null;
	     //TODO : Else, On a un remap sans node, quoi faire?
	     if (lineSplit.length != 1){
	    	 //Ici peut vouloir concat au cas où on a /image/compressed, le topic est compressed ou image/compressed?
	    	 String topicName = CanardHelper.getTopicNameFromLine(line);
	    	 currTopic = CanardHelper.getTopicFromName(topicName,currBlock);
		     if (currTopic == null){
		    	 
		    	 if (topicType == 0){
		    		 currTopic = CanardHelper.factory.createTopic();
		    	 }else{
		    		 currTopic = CanardHelper.factory.createBadTopic();
		    	 }
			     currTopic.setName(topicName);
			     incrementID();
			     currTopic.setUniqueID(uniqueID);
		     }
		     currBlock.getTopics().add(currTopic);
	     }
	     
	     return currTopic;
	}

	
	/* Find all the remaps in the text passed as parameter
	 * TODO*/
	private static void relationsFromLaunch(String text){
			
		//Doublon, to refactor
		String wordToFind = "<remap";
		Pattern word = Pattern.compile(wordToFind);
		Matcher match = word.matcher(text);
		
		while (match.find()) {

			 //Finds the end of the line
		     int endInclude = text.indexOf('>',match.end());

		     //Do not take the line into account if it's a comment
			 if (!StringUtils.checkComment(text,match.start())){
			     String temp = text.substring(match.start()+1, endInclude);
				 String from = "from=\"";
				 String to = "to=\"";
				 
			     int fromPos = temp.indexOf(from);
			     int toPos = temp.indexOf(to);
			     
			     from = temp.substring(fromPos+6,toPos-2);
			     from = from.replace("\"", "");
			     int endPos = temp.lastIndexOf("\"");
			     to = temp.substring(toPos+4,endPos);
			     to = to.replace("\"", "");
			     
			     String nametfrom = CanardHelper.getTopicNameFromLine(from);
			     String nametto = CanardHelper.getTopicNameFromLine(to);
			     
			     Topic tfrom = null;
			     Topic tto = null;
			     if (!nametfrom.equals(nametto)){
				     tfrom = linkTopicToBlock(from,1);
				     tto = linkTopicToBlock(to,1);
			     }else{
				     tfrom = linkTopicToBlock(from,0);
				     tto = linkTopicToBlock(to,0);
			     }

	
			     if (tfrom != null && tto != null){
				     //System.out.println("from : "+from);
				     //System.out.println(" to : " + to);
				     Rel r1 = CanardHelper.factory.createRel();
			    	 r1.setSrc(tfrom);
				     r1.setTgt(tto);
				     CanardHelper.model.getLinks().add(r1);
			     }
			 }

		}
		
	}
	
	private static void makeXMI() {
		// Writes a XMI with the GMF's code on the webserver
		XMIExporter.export(CanardHelper.model, OUTPUTFILE);
	
		// Create a resource set
		ResourceSet resourceSet = new ResourceSetImpl();
		
		// Register the default resource factory -- only needed for stand-alone!
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
				.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		
		// Get the URI of the model file.
		URI fileURI = URI.createFileURI(new File(OUTPUTFILE).getAbsolutePath());
	
		// Create a resource for this file.
		Resource resource = resourceSet.createResource(fileURI);
		resource.getContents().add(CanardHelper.model);
		
		// Add the initial model object to the contents.
		EObject rootObject = CanardHelper.model;
	
		// Allows to print the information to send to the HTTP request
		FileReaderTool.readFile(OUTPUTFILE);
	}


	private static void incrementID(){
		uniqueID+=1;
	}
	
	
	private static Block makeBlock(String name){
		//https://stackoverflow.com/questions/2711067/how-do-i-dynamically-name-objects-in-java
		Block b1 = CanardHelper.factory.createBlock();
		b1.setName(name);
		return b1;
	}
	
	
	//http://www.java2s.com/Tutorial/Java/0040__Data-Type/Getstheminimumofthreeintvalues.htm
	public static int minimum(int a, int b, int c) {
	      if (b < a) {
	          a = b;
	      }
	      if (c < a) {
	          a = c;
	      }
	      return a;
	}

}