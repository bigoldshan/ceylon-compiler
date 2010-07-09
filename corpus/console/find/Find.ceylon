doc "A command-line utility for searching for regular
     expression matches in files."
class Find(Process process) {
	
	try {
 		CommandLine commandLine = CommandLine(process);
		String pattern = pattern(commandLine);
		for ( File file in files(commandLine) ) {
			do (Stream stream = file.stream)
			while (stream.more) {
				String line = stream.readLine();
				if ( pattern.matches(line) ) {
					log.info("" file.name ":" stream.currentLineNumber " " line "");
				}
			}
		}
 	}
 	catch (Exception e) {
 		log.error(e.message);
 		process.exit(1);
 	}
 	
 	doc "get the pattern specified on the command line
 	     using |-pattern|"
 	String pattern(CommandLine cl) {
 		try {
			return commandLine.namedArguments["pattern"];
		}
		catch (UndefinedKeyException uke) {
			log.error("No pattern specified.");
			process.exit(1);
		}
 	}
 	
 	doc "get the file paths listed on the command line"
	List<File> files(CommandLine cl) {
		CommandLine.ListedArguments paths = cl.listedArguments
		if (paths.empty) {
			log.error("No paths specified.");
			process.exit(1);
		}
		else {
			OpenList<File> files = ArrayList<File>();
			for (String path in paths) {
				files.add( Path(path).files );
			}
			return files;
		}
	}

}