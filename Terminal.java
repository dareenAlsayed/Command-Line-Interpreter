import java.util.Scanner;        // to read text input from the user (commands typed in terminal)
import java.io.File;             // represents files and directories (used to check existence, type,..)
import java.nio.file.Path;       // represents file and directory paths in a system-independent way (modern version of file paths)
import java.nio.file.Paths;      // utility to create Path objects (Paths.get(...))
import java.io.IOException;      // signals errors related to input/output (required for file and stream operations)
import java.io.InputStream;      // base class for reading bytes (used for reading files, zips,..)
import java.io.OutputStream;     // base class for writing bytes (used for writing files, zips,..)
import java.nio.file.Files;      // performs operations on files and directories (create, copy, read, write,..)
import java.nio.file.OpenOption; // used to specify how a file is opened (for writing, appending,..)
import java.nio.file.StandardOpenOption; // enum for standard file open options (like CREATE, APPEND, WRITE, TRUNCATE_EXISTING,..)
import java.util.zip.ZipEntry;   // represents an entry (file or folder) inside a zip archive
import java.util.zip.ZipInputStream;  // reads the contents of a zip file (for extracting files)
import java.util.zip.ZipOutputStream; // writes to a zip file archive (for compressing files/folders into zip)
import java.util.Arrays;         // utility class for working with arrays (copy, sort, search,..)
import java.io.*;                // imports all classes in java.io (BufferedReader, BufferedWriter, FileReader, FileWriter,..)



class Parser {
    private String commandName;       // main command (like 'touch', 'zip', ..)
    private String[] args;            // arguments for the command
    private boolean writeRedirect;    // true if '>' is present
    private boolean appendRedirect;   // true if '>>' is present
    private String redirectTarget;    // file to redirect output to (after > or >>)

    // split the input into commandName and args
    public boolean parse(String input) {
        reset(); // clear last values (avoid confusion)

        // if the input is empty (user just pressed Enter), do nothing and return false
        if (input == null || input.trim().isEmpty()) {
            return false; // false means no command to process
        }

        // trim the input, remove leading and trailing spaces
        input = input.trim();

        // check for append '>>' before overwrite '>' to avoid confusion
        // we treat it as append, not overwrite (since '>>' would match '>' too)
        if (input.contains(" >> ")) {

            // split the input into two parts: before '>>' and after it, ex: "echo hello >> out.txt" becomes ["echo hello", "out.txt"]
            String[] parts = input.split(" >> ", 2);

            // first part is the actual command and arguments
            input = parts[0].trim();

            // second part is the filename for redirection
            redirectTarget = parts[1].trim();

            // mark that append redirection is requested
            appendRedirect = true;
        }

        // otherwise, check if the command includes overwrite redirection ' > '
        else if (input.contains(" > ")) {

            // split the input into two parts: before ' > ' and after it, ex: "echo hello > out.txt" becomes ["echo hello", "out.txt"]
            String[] parts = input.split(" > ", 2);

            // first part is the actual command and arguments
            input = parts[0].trim();

            // second part is the filename for redirection
            redirectTarget = parts[1].trim();

            // mark that overwrite redirection is requested
            writeRedirect = true;
        }

        // split the input by spaces, each word becomes an item in an array
        String[] parts = input.split("\\s+"); // "mkdir testFolder newFolder" --> ["mkdir", "testFolder", "newFolder"]

        // first part is always the command ('cd', 'ls', 'mkdir'..)
        commandName = parts[0]; // store the command as a string

        // any remaining parts are arguments (can be zero or more)
        if (parts.length > 1) {
            // copy all words after the first one as arguments
            args = new String[parts.length - 1]; // size of args array = number of parts (strings) entered - 1 (command)
            for (int i = 1; i < parts.length; i++) {
                args[i - 1] = parts[i];
            }
        } else {
            // no arguments only command so just an empty array
            args = new String[0];
        }

        // return true to show parsing was successful
        return true;
    }

    // getters
    public String getCommandName() {
        return commandName;
    }

    public String[] getArgs() {
        return args;
    }

    public boolean hasWriteRedirect() {
        return writeRedirect;
    }

    public boolean hasAppendRedirect() {
        return appendRedirect;
    }

    public String getRedirectTarget() {
        return redirectTarget;
    }

    // helper to reset everything each time you parse new input
    private void reset() {
        commandName = "";
        args = new String[0];
        writeRedirect = false;
        appendRedirect = false;
        redirectTarget = "";
    }
}


public class Terminal {
    // this variable will hold the Parser object for our Terminal to break up user input into command and arguments
    Parser parser;

    // this variable will keep track of the current working directory for our terminal
    // it starts in the folder where the program is run
    private java.nio.file.Path cwd = java.nio.file.Paths.get(System.getProperty("user.dir"));

    // constructor for Terminal, automatically creates a new Parser as part of it
    // every Terminal has its own Parser ready to use
    public Terminal() {
        parser = new Parser(); // make a new Parser object and assign it to 'parser'
    }


    // command 'exit' -> ends the program
    public void exit() {
        System.out.println("Exiting command line...");
        System.exit(0); // terminate the program
    }


    // command 'pwd' -> prints the current directory path
    public String pwd(String[] args) {
        // pwd doesn't take extra arguments, print error if any given
        if (args.length != 0) {
            return "pwd: no arguments allowed";
        }
        // return the current directory as a string
        return cwd.toString();
    }


    // command 'cd' -> changes the current working directory
    public String cd(String[] args) {
        try {
            // case 1 -> if no arguments, default to the user's home directory
            if (args.length == 0) {
                cwd = java.nio.file.Paths.get(System.getProperty("user.home")).normalize();
                return null;
            }

            // combine all arguments into a single path string for folder names with spaces like "haneen hisham"
            String arg = String.join(" ", args);

            // case 2 -> if the argument is "..", move up one directory or to parent directory
            if ("..".equals(arg)) {
                java.nio.file.Path parent = cwd.getParent();
                if (parent != null) cwd = parent.normalize();
                return null;
            }

            // create a Path object for the destination directory
            java.nio.file.Path path = java.nio.file.Paths.get(arg);

            // if the path is not absolute, resolve it based on the current directory
            if (!path.isAbsolute()) path = cwd.resolve(arg); // handle relative paths

            // normalize the path to remove any redundant elements (like "." or "..")
            path = path.normalize();

            // check if the path actually exists in the file system
            if (!java.nio.file.Files.exists(path))
                return "cd: no such file or directory: " + arg;

            // check if the path is a directory (not a file)
            if (!java.nio.file.Files.isDirectory(path))
                return "cd: not a directory: " + arg;

            // set the current directory to the new path
            cwd = path;
            return null;
        } catch (java.nio.file.InvalidPathException e) {
            // if the path given is not valid, show an error message
            return "cd: invalid path";
        }
    }


    // command 'ls' -> lists all files and folders in the current working directory
    public String ls(String[] args) {
        // if there are any arguments, show an error message
        if (args.length != 0) {
            return "ls: no arguments allowed";
        }

        // create a list to store the names of all files and directories
        java.util.List<String> names = new java.util.ArrayList<>();

        // try to open the current directory and read its contents
        try (java.nio.file.DirectoryStream<java.nio.file.Path> ds = java.nio.file.Files.newDirectoryStream(cwd)) {
            // for each entry in the directory, add its name to the list
            for (java.nio.file.Path p : ds)
                names.add(p.getFileName().toString());
        } catch (java.io.IOException e) {
            // if there is a problem reading the directory, return an error message
            return "ls: cannot read directory";
        }

        // sort the file and directory names alphabetically (case-insensitive)
        names.sort(String.CASE_INSENSITIVE_ORDER);

        // combine all names into a single string, separated by 'tab'
        return String.join("    ", names);
    }


    // command 'mkdir' -> creates one or more new directories in the current working directory or at given path
    // note: folder names/paths with spaces are NOT supported.
    public void mkdir(String[] args) {
        // if no folder name is given, show usage instructions and advice
        if (args.length == 0) {
            System.out.println("Usage: mkdir <dir1> [dir2] ...");
            System.out.println("NOTE: Folder/file names with spaces are NOT supported.");
            return;
        }

        // loop through all provided folder names or paths
        for (String path : args) {
            // if relative path, resolve it based on the current directory (cwd)
            java.nio.file.Path targetPath = cwd.resolve(path).normalize();

            // if user gave an absolute path, targetPath will just be that full path
            // create a File object for the directory
            java.io.File directory = new java.io.File(targetPath.toString());

            // check if directory already exists
            if (directory.exists()) {
                System.out.println("Directory already exists: " + path);
            }
            // try to create the directory
            else if (directory.mkdirs()) {
                System.out.println("Directory created: " + path);
            }
            // if creation failed, show an error
            else {
                System.out.println("Failed to create directory: " + path);
            }
        }
    }


    // command 'rmdir' -> removes empty directories in the current working directory or specified by path
    // usage: rmdir * OR rmdir <directory> , 'rmdir *' will remove all empty folders in the current directory
    // note: folder names/paths with spaces are NOT supported
    public void rmdir(String[] args) {
        // show usage if the user did not provide any arguments
        if (args.length == 0) {
            System.out.println("Usage: rmdir <directory> OR rmdir *");
            System.out.println("NOTE: Folder names with spaces are NOT supported.");
            return;
        }

        // if user entered 'rmdir *', remove all empty directories in the current directory
        if (args.length == 1 && args[0].equals("*")) {
            // list everything in the current working directory
            File[] files = new File(cwd.toString()).listFiles();
            if (files != null) {
                for (File f : files) {
                    // remove the item if it is an empty directory
                    if (f.isDirectory() && f.list().length == 0) {
                        if (f.delete()) {
                            System.out.println("Removed empty directory: " + f.getName());
                        }
                    }
                }
            }
        } else {
            // otherwise, try to remove the given directory if it is empty
            for (String path : args) {
                // resolve path relative to the current working directory
                File dir = new File(cwd.resolve(path).normalize().toString());

                // case 1 -> directory does not exist
                if (!dir.exists()) {
                    System.out.println("Directory not found: " + path);
                }

                // case 2 -> directory exists and is empty, so delete it
                else if (dir.isDirectory() && dir.list().length == 0) {
                    if (dir.delete()) {
                        System.out.println("Directory removed: " + path);
                    } else {
                        System.out.println("Failed to remove directory: " + path);
                    }
                }

                // case 3 -> directory isn't empty, or it's not a folder
                else {
                    System.out.println("Directory not empty or invalid path: " + path);
                }
            }
        }
    }


    // command 'touch' -> creates a new empty file in the current working directory
    // usage: touch <filename>
    // note: file names with spaces are NOT supported, supports multiple arguments not just 1
    public void touch(String[] args) {
        // if no filename is given, show usage instructions and advice
        if (args.length == 0) {
            System.out.println("Usage: touch <filename>");
            System.out.println("NOTE: File names with spaces are NOT supported.");
            return;
        }

        // loop through all file names given
        for (String name : args) {
            // resolve the file path relative to cwd
            File file = new File(cwd.resolve(name).normalize().toString());
            try {
                // case 1 -> file already exists
                if (file.exists()) {
                    System.out.println("File already exists: " + name);
                }

                // case 2 -> create a new file
                else if (file.createNewFile()) {
                    System.out.println("File created: " + name);
                }

                // case 3 -> could not create file (unknown reason)
                else {
                    System.out.println("Failed to create file: " + name);
                }
            }
            catch (java.io.IOException e) {
                // handles any error that happens when trying to create the file
                System.out.println("Error creating file: " + name + " (" + e.getMessage() + ")");
            }
        }
    }


    // command 'rm' -> removes a file (not a directory) in the current working directory or given path
    // usage: rm <filename>
    // note: file names/paths with spaces are NOT supported, supports multiple arguments not just 1
    public void rm(String[] args) {
        // if no filename given, show usage instructions
        if (args.length == 0) {
            System.out.println("Usage: rm <filename>");
            System.out.println("NOTE: File names with spaces are NOT supported.");
            return;
        }

        // loop through all file names given
        for (String name : args) {
            // Resolve the file path relative to cwd
            File file = new File(cwd.resolve(name).normalize().toString());

            // case 1 -> file does not exist
            if (!file.exists()) {
                System.out.println("File not found: " + name);
            }

            // case 2 -> is a directory, not a file
            else if (file.isDirectory()) {
                System.out.println("Cannot remove directory with rm: " + name);
            }

            // case 3 -> try to delete the file
            else {
                if (file.delete()) {
                    System.out.println("File deleted: " + name);
                } else {
                    System.out.println("Failed to delete file: " + name);
                }
            }
        }
    }


    // converts a relative or absolute path to a normalized Path based on cwd
    private Path resolvePath(String p) {
        Path path = Paths.get(p);
        if (!path.isAbsolute()) path = cwd.resolve(p);
        return path.normalize();
    }


    // command '>' -> overwrites output to a file (used for > redirection)
    // if the file doesn't exist, it will be created. if it does, its content will be replaced
    private void writeOverwrite(String fileName, String text) throws IOException {
        // convert the fileName String into a Path object, resolving it to an absolute/normalized path
        Path path = resolvePath(fileName);

        // find the parent directory (folder) that will contain the file
        Path parent = path.getParent();

        // if the parent folder does not exist, create it (including all necessary parent folders)
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        // options for writing:
        // - CREATE: if the file does not exist, create it
        // - TRUNCATE_EXISTING: if the file exists, erase its content
        // - WRITE: write to the file
        OpenOption[] opts = {
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        };

        // actually write the text (plus a system-specific new line) into the file
        Files.write(path, (text + System.lineSeparator()).getBytes(), opts);
    }


    // command '>>' -> appends output to a file (used for >> redirection)
    // appends the given text to the end of a file if it exists, or creates it if it doesn't exist
    private void writeAppend(String fileName, String text) throws IOException {
        // convert the fileName String into a Path object, resolving it to an absolute/normalized path
        Path path = resolvePath(fileName);

        // find the parent directory (folder) that will contain the file
        Path parent = path.getParent();

        // if the parent folder does not exist, create it (including all necessary parent folders)
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        // options for writing:
        // - CREATE: if the file does not exist, create it
        // - APPEND: add text to the end instead of replacing the whole file
        // - WRITE: write to the file
        OpenOption[] opts = {
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
                StandardOpenOption.WRITE
        };

        // actually append the text (plus a system-specific new line) to the end of the file
        Files.write(path, (text + System.lineSeparator()).getBytes(), opts);
    }


    // command 'zip' -> zip or compress files and directories
    // supports two usages:
    //   zip archive.zip file1 file2 ...        (zips individual files)
    //   zip -r archive.zip folder              (zips a whole folder, recursively)
    private void zip(String[] args) throws IOException {
        // if not enough arguments, show usage instructions and exit
        if (args.length < 2) {
            System.out.println("usage: zip archive.zip file1 [file2 ...]  or  zip -r archive.zip folder");
            return;
        }

        // handle the recursive folder zipping: zip -r archive.zip folder
        if ("-r".equals(args[0])) {
            // check that there are exactly three arguments: -r archive.zip folder
            if (args.length != 3) {
                System.out.println("usage: zip -r archive.zip folder");
                return;
            }

            // resolve the archive file path and the folder to zip
            Path zipPath = resolvePath(args[1]);
            Path folder  = resolvePath(args[2]);

            // check if the given path is a directory
            if (!Files.isDirectory(folder)) {
                System.out.println("not a folder: " + folder);
                return;
            }

            // open a new ZipOutputStream to write to the zip file
            ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath));
            // use a helper to add all files/subfolders (recursive)
            addFolderToZip(zos, folder.toFile(), folder.toFile().getAbsolutePath());
            zos.close();

            System.out.println("zip created: " + zipPath.getFileName());
            return;
        }

        // handle the normal case: zip archive.zip file1 file2 ...
        // the first argument is the archive file name
        Path zipPath = resolvePath(args[0]);
        ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath));
        byte[] buffer = new byte[1024]; // buffer for copying file data

        // loop through each file to add to the zip
        for (int i = 1; i < args.length; i++) {
            Path filePath = resolvePath(args[i]);

            // only include files (skip directories and missing files)
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                System.out.println("skipped: " + filePath);
                continue;
            }

            // create a new entry in the zip with just the file's name (not full path)
            ZipEntry entry = new ZipEntry(filePath.getFileName().toString());
            zos.putNextEntry(entry);

            // open the file for reading and write its bytes into the zip
            InputStream in = Files.newInputStream(filePath);
            int len;
            while ((len = in.read(buffer)) > 0)
                zos.write(buffer, 0, len);

            in.close();
            zos.closeEntry(); // finish writing this file into the zip
        }
        zos.close(); // close the zip archive

        System.out.println("zip created: " + zipPath.getFileName());
    }


    // zip helper for recursive (-r), used by zip -r to include all files and subfolders
    // recursively adds a folder and its contents to a ZipOutputStream
    private void addFolderToZip(ZipOutputStream zos, java.io.File folder, String basePath) throws IOException {
        // get all files and subfolders in the given folder
        java.io.File[] files = folder.listFiles();
        if (files == null) return; // if the folder is empty or error reading, stop here

        byte[] buffer = new byte[1024]; // buffer for reading file data

        // loop through each file/subfolder inside the current folder
        for (java.io.File f : files) {
            // build a relative path for the zip entry (so folders/files inside folders are zipped correctly)
            String relPath = f.getAbsolutePath().substring(basePath.length());

            // remove leading path separator, if present
            if (relPath.startsWith(java.io.File.separator)) relPath = relPath.substring(1);

            // use forward slashes (/) for zip format (works on all operating systems)
            relPath = relPath.replace(java.io.File.separatorChar, '/');

            if (f.isDirectory()) {
                // if it's a directory, add an entry for the folder (ensures empty folders are stored in the zip)
                if (!relPath.endsWith("/")) relPath += "/";
                zos.putNextEntry(new ZipEntry(relPath));
                zos.closeEntry();
                // recurse: add all its inner files/folders
                addFolderToZip(zos, f, basePath);
            }
            else {
                // if it's a file, add a new entry for this file
                zos.putNextEntry(new ZipEntry(relPath));
                InputStream in = new java.io.FileInputStream(f);
                int len;
                // read the file and write its bytes into the zip
                while ((len = in.read(buffer)) > 0)
                    zos.write(buffer, 0, len);
                in.close();
                zos.closeEntry(); // done with this file
            }
        }
    }


    // command 'unzip' -> unzip or extract contents of a zip file into the current directory or a specified destination
    // usage: unzip archive.zip                 (extracts into the current directory)
    //        unzip archive.zip -d folderName   (extracts into the folderName directory)
    private void unzip(String[] args) throws IOException {
        // check argument count: need at least the zip file name
        if (args.length < 1) {
            System.out.println("usage: unzip archive.zip [-d destination]");
            return;
        }

        // resolve the path to the zip file (archive)
        Path zipPath = resolvePath(args[0]);
        // by default, extract into current working directory (cwd)
        Path dest    = cwd;

        // if '-d' and a destination folder are given, change the extraction destination
        if (args.length >= 3 && "-d".equals(args[1]))
            dest = resolvePath(args[2]);

        // make the destination folder if it doesn't exist yet
        if (!Files.exists(dest)) Files.createDirectories(dest);

        // set up the input stream to read zip file entries (the files and folders inside the archive)
        ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath));
        ZipEntry entry;
        byte[] buffer = new byte[1024]; // buffer to copy bytes

        // loop through every item in the archive
        while ((entry = zis.getNextEntry()) != null) {
            // build the full output path for this entry (file or folder)
            Path outPath = dest.resolve(entry.getName()).normalize();

            // security: prevents extracting outside the destination folder
            // (avoids zip slip attacks where entry names have '../' etc.)
            if (!outPath.startsWith(dest)) {
                System.out.println("blocked suspicious entry: " + entry.getName());
                zis.closeEntry();
                continue;
            }

            // if it's a directory, make sure the folder exists
            if (entry.isDirectory()) {
                Files.createDirectories(outPath);
            } else {
                // if it's a file, make sure its parent folder exists
                if (outPath.getParent() != null && !Files.exists(outPath.getParent()))
                    Files.createDirectories(outPath.getParent());

                // write the file's bytes to disk
                OutputStream out = Files.newOutputStream(
                        outPath,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE
                );
                int len;
                while ((len = zis.read(buffer)) > 0)
                    out.write(buffer, 0, len);
                out.close();
            }
            // done with this entry—move to the next one
            zis.closeEntry();
        }
        // done reading the zip file—close input stream
        zis.close();
        System.out.println("unzipped into: " + dest);
    }


    // command 'echo'-> prints the arguments as a single line of text
    // usage: echo hello world
    public String echo(String[] args) {
        // join all arguments with spaces (so echo writes everything, including spaces, like a sentence)
        return String.join(" ", args);
    }


    // command 'cat' -> prints the contents of one or more files to the terminal
    // usage: cat <file1> [file2 ...]
    public void cat(String[] args) {
        // check that the user provided at least one file name
        if (args.length == 0) {
            System.out.println("Error: Please enter at least one file name");
            System.out.println("Usage: cat <file1> [file2 ...]");
            return;
        }

        // go through each file name provided and attempt to print its contents
        for (String fileName : args) {
            // resolve relative to the current working directory (cwd)
            File file = new File(cwd.resolve(fileName).normalize().toString());

            // check if the file actually exists
            if (!file.exists()) {
                System.out.println("Error: File not found " + fileName);
                continue;
            }

            // don't allow folders in cat command
            if (file.isDirectory()) {
                System.out.println("Error: " + fileName + " is a directory, not a file.");
                continue;
            }

            // read and print the file contents line by line
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
                br.close();
            } catch (IOException e) {
                System.out.println("Error: Unable to read " + fileName);
            }
        }
    }


    // command 'cp' -> copies the contents of a source file to a destination file
    // usage: cp <sourceFile> <destinationFile>
    public void cp(String[] args) {
        // check the user passed the correct number of arguments (must be 2: source and destination)
        if (args.length != 2) {
            System.out.println("Error: cp needs 2 files");
            System.out.println("Usage: cp <sourceFile> <destinationFile>");
            return;
        }

        // get the source and destination file paths, making sure they are relative to cwd
        File source = new File(cwd.resolve(args[0]).normalize().toString());
        File destination = new File(cwd.resolve(args[1]).normalize().toString());

        // confirm that the source file exists
        if (!source.exists()) {
            System.out.println("Error: Source file not found " + args[0]);
            return;
        }

        // check that the source is a file (not a folder)
        if (source.isDirectory()) {
            System.out.println("Error: Source must be a file, not a directory.");
            return;
        }

        // start copying: read from source, write to destination (overwrite/create)
        try {
            // this will read the source file line by line
            BufferedReader reader = new BufferedReader(new FileReader(source));

            // this will write to the destination file, creating or overwriting
            BufferedWriter writer = new BufferedWriter(new FileWriter(destination));

            String line; // store each line temporarily for copying
            while ((line = reader.readLine()) != null) {
                writer.write(line);   // write the line to destination
                writer.newLine();     // add a new line (keep format the same)
            }

            // close resources after we're done (important for saving and avoiding file lock issues)
            reader.close();
            writer.close();

            // success message to user
            System.out.println("File copied successfully from " + args[0] + " to " + args[1]);
        } catch (IOException e) {
            // catch and display any file-related problems (permissions, disk full, etc.)
            System.out.println("Error: Could not copy the file.");
        }
    }


    // command 'cp -r' -> recursively copy folder contents from source to destination
    // usage: cp -r <sourceFolder> <destinationFolder>
    public void cpr(String[] args) {
        // make sure the user provided exactly 2 arguments (source and destination folders)
        if (args.length != 2) {
            System.out.println("Error: cp -r needs 2 folders");
            System.out.println("Usage: cp -r <sourceFolder> <destinationFolder>");
            return;
        }

        // resolve folder paths relative to current working directory (cwd)
        File sourceFolder = new File(cwd.resolve(args[0]).normalize().toString());
        File destinationFolder = new File(cwd.resolve(args[1]).normalize().toString());

        // check that the source folder exists and is a directory
        if (!sourceFolder.exists()) {
            System.out.println("Error: Source folder not found " + args[0]);
            return;
        }
        if (!sourceFolder.isDirectory()) {
            System.out.println("Error: Source must be a folder, not a file.");
            return;
        }

        // check that destination folder already exists and is a directory
        if (!destinationFolder.exists()) {
            System.out.println("Error: Destination folder does not exist. Please create it first.");
            return;
        }
        if (!destinationFolder.isDirectory()) {
            System.out.println("Error: Destination path must be a folder, not a file.");
            return;
        }

        // start the recursive copy
        try {
            copyFolderContents(sourceFolder, destinationFolder);
            System.out.println("Folder copied successfully.");
        } catch (IOException e) {
            System.out.println("Error while copying folder.");
        }
    }


    // helper method for copying files and subfolders
    private void copyFolderContents(File source, File destination) throws IOException {
        // list all items (files and folders) inside the source directory
        File[] items = source.listFiles();
        if (items == null) return;

        for (File item : items) {
            File newLocation = new File(destination, item.getName());

            if (item.isDirectory()) {
                // if it's a folder, make sure it exists in the destination
                if (!newLocation.exists()) {
                    // only copy into existing folders (skip new subfolders)
                    System.out.println("Skipping folder: " + item.getName() + " (not found in destination)");
                    continue;
                }
                // recursively copy the subfolder
                copyFolderContents(item, newLocation);
            } else {
                // if it's a file, copy it to the new location
                copyFile(item, newLocation);
            }
        }
    }


    // helper method for copying a single file
    private void copyFile(File sourceFile, File destFile) throws IOException {
        // this will read the source file line by line
        BufferedReader reader = new BufferedReader(new FileReader(sourceFile));

        // this will write to the destination file, creating or overwriting
        BufferedWriter writer = new BufferedWriter(new FileWriter(destFile));

        String line;
        while ((line = reader.readLine()) != null) {
            writer.write(line);
            writer.newLine();
        }

        // close resources after we're done (important for saving and avoiding file lock issues)
        reader.close();
        writer.close();
    }


    // command 'wc' -> shows the number of lines, words, and characters in a file
    // usage: wc <file>
    public void wc(String[] args) {
        // check that the user passed exactly one argument (a file name)
        if (args.length != 1) {
            System.out.println("Error: wc needs exactly 1 file name");
            System.out.println("Usage: wc <file>");
            return;
        }

        // resolve the file path relative to the current directory (cwd)
        File file = new File(cwd.resolve(args[0]).normalize().toString());

        // check the file exists
        if (!file.exists()) {
            System.out.println("Error: File not found " + args[0]);
            return;
        }

        // check that it is not a directory
        if (file.isDirectory()) {
            System.out.println("Error: " + args[0] + " is a directory, not a file.");
            return;
        }

        // counters for lines, words, and characters
        int lines = 0;
        int words = 0;
        int chars = 0;

        try {
            // reader for the file content
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            // loop through and count for each line in the file
            while ((line = br.readLine()) != null) {
                lines++;    // count lines

                // trim leading/trailing spaces and split by one or more spaces to count words
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    String[] wordArray = trimmed.split("\\s+");
                    words += wordArray.length;
                }

                chars += line.length(); // count characters (not including newline)
            }

            br.close();

            // output format: numOfLines  numOfWords  numOfCharacters  filename
            System.out.println(lines + "  " + words + "  " + chars + "  " + file.getName());
        } catch (IOException e) {
            System.out.println("Error: Could not read file.");
        }
    }


    // this method decides which command to run based on what user typed
    public void chooseCommandAction() {
        // get the command name from the parser
        String command = parser.getCommandName();

        // get the arguments (can be zero or more)
        String[] arguments = parser.getArgs();

        switch (command) {
            case "exit":
                exit();
                break;
            case "pwd":
                String out = pwd(arguments); // run the 'pwd' method
                if (out != null && !out.isEmpty()) {
                    System.out.println(out); // print its output
                }
                break;
            case "cd":
                String cdOut = cd(arguments); // run the 'cd' method
                if (cdOut != null && !cdOut.isEmpty()) {
                    System.out.println(cdOut); // print its output
                }
                break;
            case "ls":
                String lsOut = ls(arguments); // run the 'ls' method
                if (lsOut != null && !lsOut.isEmpty()) {
                    System.out.println(lsOut); // print its output
                }
                break;
            case "mkdir":
                mkdir(arguments); // this command prints its own messages
                break;
            case "rmdir":
                rmdir(arguments);
                break;
            case "touch":
                touch(arguments);
                break;
            case "rm":
                rm(arguments);
                break;
            case "zip":
                try { zip(arguments); }
                catch (IOException e) { System.out.println("zip error: " + e.getMessage()); }
                break;
            case "unzip":
                try { unzip(arguments); }
                catch (IOException e) { System.out.println("unzip error: " + e.getMessage()); }
                break;
            case "echo":
                String echoOut = echo(arguments);
                if (echoOut != null && !echoOut.isEmpty()) {
                    System.out.println(echoOut);
                }
                break;
            case "cat":
                cat(arguments);
                break;
            case "cp":
                // user typed cp -r sourceFolder destinationFolder
                if (arguments.length > 0 && arguments[0].equals("-r")) {
                    String[] argsForCpr = Arrays.copyOfRange(arguments, 1, arguments.length);
                    cpr(argsForCpr); // call the recursive copy function
                } else {
                    cp(arguments);   // regular file copy
                }
                break;
            case "wc":
                wc(arguments);
                break;
            default:
                // if the command is unknown, print an error
                System.out.println("Unknown command: " + command);
        }

    }


    public static void main(String[] args) {
        // make a Terminal object, which will control running the commands
        Terminal terminal = new Terminal();

        // make a Scanner object to read input that the user types in the console
        Scanner scanner = new Scanner(System.in);

        // start an endless loop (loop will break only if the user types 'exit')
        while (true) {
            // show the user a prompt (just like in real command lines)
            System.out.print("> ");

            // read one line of input from the user (then user hits Enter)
            String input = scanner.nextLine();

            // use the parser to split up the input into command, arguments, and redirection info
            boolean parsed = terminal.parser.parse(input);

            if (!parsed) {
                // user pressed enter or typed only spaces
                System.out.println("Error: No command entered.");
                // continue on to next loop iteration (shows prompt again)
                continue;
            }

            // gather redirection info from the parser (handles > and >>)
            boolean writeRedirect = terminal.parser.hasWriteRedirect();   // true for >
            boolean appendRedirect = terminal.parser.hasAppendRedirect(); // true for >>
            String redirectTarget = terminal.parser.getRedirectTarget();  // filename after > or >>

            // get the command and its arguments
            String command = terminal.parser.getCommandName();
            String[] arguments = terminal.parser.getArgs();

            String result = null; // holds any output text from commands

            // for commands that SHOULD support output redirection, capture their output
            // these are pwd, cd, ls, and echo (the commands that produce text output)
            // other commands print directly and don't need redirection
            switch (command) {
                case "pwd":
                    result = terminal.pwd(arguments);
                    break;
                case "cd":
                    result = terminal.cd(arguments);
                    break;
                case "ls":
                    result = terminal.ls(arguments);
                    break;
                case "zip":
                    try { terminal.zip(arguments); }
                    catch (IOException e) { System.out.println("zip error: " + e.getMessage()); }
                    break;
                case "unzip":
                    try { terminal.unzip(arguments); }
                    catch (IOException e) { System.out.println("unzip error: " + e.getMessage()); }
                    break;
                case "echo":
                    result = terminal.echo(arguments);
                    break;
                default:
                    // any other command (mkdir, rm, touch, rmdir), just run and let it print its own output
                    terminal.chooseCommandAction();
            }

            // handle output redirection if requested and only for commands that produce output text (ls, cd, pwd)
            // for other commands, result will be null and nothing will happen here
            try {
                if (writeRedirect && redirectTarget != null && result != null && !result.isEmpty()) {
                    // if '>', write to file (overwrite)
                    terminal.writeOverwrite(redirectTarget, result);
                }
                else if (appendRedirect && redirectTarget != null && result != null && !result.isEmpty()) {
                    // if '>>', append to file
                    terminal.writeAppend(redirectTarget, result);
                }
                else if (result != null && !result.isEmpty()) {
                    // otherwise, just print to console
                    System.out.println(result);
                }
            } catch (IOException e) {
                System.out.println("Redirection failed: " + e.getMessage());
            }
        }

    }

}

/*  Sample Output:

> pwd
C:\Users\haneen hisham\IdeaProjects\OS_Assignment1
> ls
.gitignore    .idea    OS_Assignment1.iml    out    src
> mkdir testFolder
Directory created: testFolder
> touch file1.txt
File created: file1.txt
> echo Hello World > file1.txt
> cat file1.txt
Hello World
> touch file2.txt
File created: file2.txt
> echo Operating Systems CLI > file2.txt
> cp file1.txt file3.txt
File copied successfully from file1.txt to file3.txt
> cat file3
Error: File not found file3
> cat file3.txt
Hello World
> echo This is a demo >> file3.txt
> cat file3.txt
Hello World
This is a demo
> wc file3.txt
2  6  25  file3.txt
> rm file1.txt
File deleted: file1.txt
> mkdir testFolder/subFolder
Directory created: testFolder/subFolder
> touch testFolder/subFolder/sample.txt
File created: testFolder/subFolder/sample.txt
> echo Demo test > testFolder/subFolder/sample.txt
> cat testFolder/subFolder/sample.txt
Demo test
> mkdir testFolder2
Directory created: testFolder2
> rmdir testFolder2
Directory removed: testFolder2
> zip
usage: zip archive.zip file1 [file2 ...]  or  zip -r archive.zip folder
> ls
.gitignore    .idea    file2.txt    file3.txt    OS_Assignment1.iml    out    src    testFolder
> zip testFolder.zip testFolder
skipped: C:\Users\haneen hisham\IdeaProjects\OS_Assignment1\testFolder
zip created: testFolder.zip
> ls
.gitignore    .idea    file2.txt    file3.txt    OS_Assignment1.iml    out    src    testFolder    testFolder.zip
> unzip
usage: unzip archive.zip [-d destination]
> unzip testFolder.zip -d unpackedFolder
unzipped into: C:\Users\haneen hisham\IdeaProjects\OS_Assignment1\\unpackedFolder
> ls
.gitignore    .idea    file2.txt    file3.txt    OS_Assignment1.iml    out    src    testFolder    testFolder.zip    unpackedFolder
> rmdir testFolder/subFolder
Directory not empty or invalid path: testFolder/subFolder
> cd testFolder/subFolder
> ls
sample.txt
> rm sample.txt
File deleted: sample.txt
> cd ..
> rmdir subFolder
Directory removed: subFolder
> ls
> cd ..
> pwd
C:\Users\haneen hisham\IdeaProjects\OS_Assignment1
> ls
.gitignore    .idea    file2.txt    file3.txt    OS_Assignment1.iml    out    src    testFolder    testFolder.zip    unpackedFolder
> rm file2.txt file3.txt
File deleted: file2.txt
File deleted: file3.txt
> ls
.gitignore    .idea    OS_Assignment1.iml    out    src    testFolder    testFolder.zip    unpackedFolder
> pwd
C:\Users\haneen hisham\IdeaProjects\OS_Assignment1
> cd ../../../
> pwd
C:\Users
> exit
Exiting command line...

*/
