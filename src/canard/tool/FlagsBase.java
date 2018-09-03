package canard.tool;
import canard.tool.CanardTool;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import canard.CanardFactory;
import canard.CanardModel;
import canard.Flag;

public class FlagsBase {
	static Map<String, ArrayList<String>> dict = new HashMap<String, ArrayList<String>>();
	public static final String INPUTFLAGS = "input/flags.txt";
	
	
	private static String[] formatArg(String line){
		line = line.replace("\t", "");
		line = line.replace("\"/>", "\" />"); //Add space if absent for splitting
		String[] parts = line.split(" ");
		return parts;
	}
	
	// Fetches the flag's value depending on the search parameter
	// Search is either name= or value=
	public static String getArgInfo(String line, String search){
		String[] parts = formatArg(line);
		String flagInfo = "";
		for (int i=0; i<parts.length;i++){
			if (parts[i].contains(search)){
				flagInfo = parts[i].replace(search,"");
				flagInfo = flagInfo.replaceAll("\"", "");
			}
		}
		return flagInfo;
	}

	public static void generateFlagsText( String text){
		initDict();		
		String flagname = getArgInfo(text, "name=");
		String category = findCategory(flagname);
		Flag catFlag = getFlagByName(category);
		if (catFlag == null){
			//Category doesn't exist yet, must create it
			catFlag = makeFlag(category,true);
			CanardHelper.model.getFlags().add(catFlag);
		}
		
		//Create arg flag
		Flag currFlag = makeFlag(flagname, false);
		
		//Add flags to the model
		CanardHelper.model.getFlags().add(currFlag);
		
		//Link category and flag
		catFlag.getChild().add(currFlag);

	}
	
	public static Flag getFlagByName(String flagName){
		for (Flag f : CanardHelper.model.getFlags()){
			if (f.getName().equals(flagName)){
				return f;
			}
			for (Flag child : f.getChild()){
				if (child.getName().equals(flagName)){
					return child;
				}
			}
		}
		return null;
	}
	private static String findCategory(String arg){
		String category = "Undefined";
		for (Map.Entry<String, ArrayList<String>> entry : dict.entrySet()) {
		    String key = entry.getKey();
		    ArrayList<String> value = entry.getValue();
		    for (String v : value){
			    if (arg.contains(v)){
			    	return key;
			    }	
		    }

		}
		return category;
	}
	
	// Hardcoded dict that can be changed
	private static void initDict(){
		int i = 0;
		ArrayList<String> temp = new ArrayList<String>();
		temp.addAll(Arrays.asList("camera","localization","navigation","avoidance","lane_following","apriltags","joystick","intersectiontype", "coordination", "visualization",
				"wheels", "anti_instagram", "LED", "map_name", "verbose"));
		
		dict.put("Camera",new ArrayList<String>());
		dict.get("Camera").add(temp.get(i));
		i++;
		
		dict.put("Localization",new ArrayList<String>());
		dict.get("Localization").add(temp.get(i));
		i++;
		
		dict.put("Navigation",new ArrayList<String>());
		dict.get("Navigation").add(temp.get(i));
		i++;
		
		dict.put("Avoidance",new ArrayList<String>());
		dict.get("Avoidance").add(temp.get(i));
		i++;
		
		dict.put("LaneFollowing",new ArrayList<String>());
		dict.get("LaneFollowing").add(temp.get(i));
		i++;
		
		dict.put("Apriltags",new ArrayList<String>());
		dict.get("Apriltags").add(temp.get(i));
		i++;
		
		dict.put("Control",new ArrayList<String>());
		dict.get("Control").add(temp.get(i));
		i++;
		dict.get("Control").add(temp.get(i));
		i++;
		dict.get("Control").add(temp.get(i));
		i++;
		dict.get("Control").add(temp.get(i));
		i++;
		
		dict.put("Specific",new ArrayList<String>());
		dict.get("Specific").add(temp.get(i));
		i++;
		dict.get("Specific").add(temp.get(i));
		i++;
		dict.get("Specific").add(temp.get(i));
		i++;
		dict.get("Specific").add(temp.get(i));
		i++;
		dict.get("Specific").add(temp.get(i));
		i++;

	}
	
	
	// Creates the EObject associated with the parameters
	private static Flag makeFlag(String flagName, boolean abstractVal){
		Flag f1 = CanardHelper.factory.createFlag();
		f1.setName(flagName);
		f1.setIsAbstract(abstractVal); // True if category, False if feature
		return f1;
	}

	
}
