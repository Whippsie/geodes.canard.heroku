package canard.tool;

import org.eclipse.emf.common.util.EList;

import canard.Block;
import canard.CanardFactory;
import canard.CanardModel;
import canard.Topic;

public class CanardHelper {
	public static CanardModel model;
	public static CanardFactory factory;
	
	
	public static void initModel(){
		CanardHelper.factory = CanardFactory.eINSTANCE;
		model = factory.createCanardModel();
	}
	
	
	/* Loops through all topics related to a block
	 * Returns the topic associated with a name, or null */
	public static Topic getTopicFromName(String name, Block b){
		for (Topic top : b.getTopics()){
			if (top.getName().equals(name)){
				return top;
			}
		}
	return null;
	}
	
	
	/* Fetch the full name of a topic given a line
	 * Manages the case where the topic name has multiple levels
	 * Such as camera_node/image/camera_info */
	public static String getTopicNameFromLine(String line){
		String[] nodeTopic = line.split("/");
	     
	     //TODO : for now ignore the $
	     for (int i =0; i<nodeTopic.length;i++){
	    	 if (nodeTopic[i].contains("$")){
	    		 return null;
	    	 }
	     }
	     String val = "";
	     // If more than 2 elements, stick them together in the topic name
	     if (nodeTopic.length > 2){
	    	 return StringUtils.concat(nodeTopic);
	     }else if (nodeTopic.length >1){
	    	 return nodeTopic[1];
	     }else {
	    	 // If only 1 element, suppose this is the topic's name
	    	 return nodeTopic[0];
	     }
	}
	
	
	/* Loops through all blocks related to the model
	 * Returns the block associated with a name, or null */
	public static Block getBlockFromName(String name){
		EList<Block> listBlock = model.getBlocks();
		for (Block b : listBlock){
			if (b.getName().matches(name)){
				return b;
			}
		}
		return null;
		
	}
}
