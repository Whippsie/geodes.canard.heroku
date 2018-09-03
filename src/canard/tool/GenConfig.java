package canard.tool;
import canard.tool.CanardTool;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.emf.common.util.EMap;

import canard.CanardFactory;
import canard.CanardModel;
import canard.Configuration;
import canard.Flag;
import canard.tool.FlagsBase;
public class GenConfig {
	
	/* Called only the first time we read the file 
	 * Generates a configuration and add it to the model 
	 * Returns the config generated */
	public static Configuration genConfigText(String text){
		Configuration config = CanardHelper.factory.createConfiguration();
		adaptConfig(text, config);
		CanardHelper.model.getConfigurations().add(config);
		return config;
	}
	

	/* Given a text, parse every argument and create the flags
	 * Also called to modify the configuration afterwards */
	public static void adaptConfig(String text, Configuration config){
		String[] lines = splitLines(text);
		
		// Get current flags list
		EMap<Flag, String> flags = config.getConfigflags();
		
		for (int i=0 ; i < lines.length ; i++){
			
			// Fetch the name, value and possible default value of the argument
			String argName = FlagsBase.getArgInfo(lines[i],"name=");
			String argValue = "";
			
			// We prioritize the value given to the default value
			if (lines[i].contains("value=")){
				argValue = FlagsBase.getArgInfo(lines[i],"value=");
			}else if(lines[i].contains("default=")){
				argValue = FlagsBase.getArgInfo(lines[i],"default=");
			}

			// Both information are present
			if (!argName.equals("") && !argValue.equals("")){
				
				// Check if the flag already exists
				Flag f = FlagsBase.getFlagByName(argName);
				if (f == null){
					// Given a line, generates a flag and get the object
					FlagsBase.generateFlagsText(lines[i]);
					f = FlagsBase.getFlagByName(argName);
				}
				
				// Add the new flag to the flag list
				// If the flag already exists, will override the value
				flags.put(f,argValue);
			}
			
			// Add all the mappings to the configuration
			config.getConfigflags().putAll(flags);

		}
	}
	public static String[] splitLines(String text){
		return text.split("\n");
	}
}
