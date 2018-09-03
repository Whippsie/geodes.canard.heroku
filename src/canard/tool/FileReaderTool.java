package canard.tool;

import java.nio.file.Files;
import java.nio.file.Paths;

public class FileReaderTool {
	
	
	public static String readFile(String filePath){
		String text = "";
		try {
			//Reads the whole file
			text = new String(Files.readAllBytes(Paths.get(filePath)), "UTF-8");
			
			// Allows to send the information with stdout to the website through HTTP 
			// Represents a backup, in case the file cannot be written in the webserver
			if (filePath.contains("gmfXMI")) {
				System.out.println(text);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return text;
	}
}
