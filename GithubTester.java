import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.io.Serializable;

public class GithubTester {
    public static void main(String[] args) throws Exception {
        //resetDirectories();
        Github.initializeDirs();
        //testIndexing();
        testMakeTree();
    }

    // DIRECTORY TESTERS
    // tests if the directory exists
    public static boolean testDirExistence() {
        File[] files = Github.createDirList();
        boolean[] doesExist = new boolean[4];
        boolean conclusion = true;

        for (int i = 0; i < 4; i++) {
            doesExist[i] = files[i].exists();
            conclusion = conclusion && files[i].exists();
        }

        System.out.println("~ Existing Directories ~\nGit: " + doesExist[0] + "\nObjects: " + doesExist[1] + "\nIndex: "
                + doesExist[2] + "\nHead: " + doesExist[3]);

        return conclusion;
    }

    // resets all directories
    public static void resetDirectories() {
        File dir = new File("./git");
        if (dir.exists()) {
            deleteAllFiles(dir);
            System.out.println("Files sucessfully deleted.");
            dir.delete();
        } else {
            System.out.println("Main directory could not be located.");
        }
    }

    // recursively deletes files
    public static void deleteAllFiles(File dir) {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                deleteAllFiles(f);
            }
            f.delete();
        }
    }

    // creates and deletes ./git dirs the number of times indicated by the argument.
    public static void dirTest(int numCycles) {
        File git = new File("./git");
        if (git.exists()) {
            resetDirectories();
        }

        for (int i = 0; i < numCycles; i++) {
            try {
                System.out.println("Trial " + (i + 1) + ": ");
                Github.initializeDirs();
                resetDirectories();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // BLOB
    // creates a blob using file data input, returns true if worked and false if
    // not. resets Blob file after.
    public static boolean doesFileBLOB(File f) {
        if (!f.exists()) {
            Github.createBLOBfile(f);

            String dir = "./git/objects/";
            String contents = Github.readFile(f);
            String hash = Github.hashFile(contents);
            File blob = new File(dir + hash);

            if (blob.exists()) {
                resetBlob();
                return true;
            } else {
                resetBlob();
                return false;
            }
        }
        System.out.println("File already exists in Blob. Please reset.");
        return false;

    }

    public static void resetBlob() {

        // reset index
        File index = new File("./git/index");
        if (index.exists()) {
            if (index.delete()) {
                try {
                    index.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // reset BLOB files
        File objs = new File("./git/objects");
        if (objs.exists()) {
            deleteAllFiles(objs);
        }
    }

    // INDEX
    // creates 4 files & checks if the blobbing works correctly and the indexing
    // works (using the noncompressed version of SHA1)
    public static void testIndexing() throws IOException {

        // creating test files
        String dir = "./git/";
        String[] files = new String[] { "f1.txt", "f2.txt", "f3.txt", "f4.txt" };
        String[] contents = new String[] { "eeny", "meeny", "miny", "moe" };
        String[] sha1 = new String[] { "4140d3efad5acc01cc34b58ee88fc6c21568262d",
                "42a9a4ef58698cb708e9fefba06bd49ad02e05e1", "03516b717bbb8ca16a73ec06f236d0bc7d4120cb",
                "63e885ca488b7659504b5878d017e2a196f4475e" };

        for (int i = 0; i < 4; i++) {
            File f = new File(dir + files[i]);
            Github.fileWriter(contents[i], f);
            Github.createBLOBfile(f);
            Github.updateIndex(Github.hashFile(contents[i]), files[i]);
        }

        // checking validity
        dir = "./git/objects/";
        System.out.println("Was correct SHA-1 file blobbed in objects?");
        for (int i = 0; i < 4; i++) {
            System.out.println(files[i] + ": " + (new File(dir + "" + sha1[i])).exists());
        }

        System.out.println();
        System.out.println("Was index updated correctly?");
        BufferedReader br = new BufferedReader(new FileReader(new File("./git/index")));
        while (br.ready()) {
            for (int i = 0; i < 4; i++) {
                String index = br.readLine();
                String fileName = index.substring((index.length() - 6));
                String hash = index.substring(0, index.length() - 7);
                System.out.println(files[i] + ":");
                System.out.println("SHA-1: " + hash.equals(sha1[i]));
                System.out.println("File Name: " + fileName.equals(files[i]));
                System.out.println();
            }
        }
        br.close();

    }

    // deletes non-essential files in git folder (not within objs)
    public static void deleteTestFiles() {
        String dir = "./git";
        ArrayList<String> basicGitDirs = new ArrayList<>(3);
        basicGitDirs.add("objects");
        basicGitDirs.add("HEAD");
        basicGitDirs.add("index");
        String[] currentFiles = (new File(dir)).list();

        for (String file : currentFiles) {
            if (!basicGitDirs.contains(file)) {
                (new File(dir + "/" + file)).delete();
            }
        }
    }

    public static void resetToMyStandardsAKABareMinimum() {
        resetBlob();
        deleteTestFiles();
    }

    

    // MAKE TREE
    public static void testMakeTree() throws Exception {
        generateTestFiles();
    
        String rootSha = Github.makeTree(new File("."));
        System.out.println("Root tree SHA: " + rootSha);
    
        File file3 = new File("testFolder/file3.txt");
        File file4 = new File("testFolder/subFolder/file4.txt");
    
        String sha3 = Github.hashFile(Github.readFile(file3));
        String sha4 = Github.hashFile(Github.readFile(file4));
    
        boolean allGood = true;
    
        if (!new File("./git/objects/" + sha3).exists()) {
            System.out.println("Missing blob for file3.txt");
            allGood = false;
        }
    
        if (!new File("./git/objects/" + sha4).exists()) {
            System.out.println("Missing blob for file4.txt");
            allGood = false;
        }
    
        if (!new File("./git/objects/" + rootSha).exists()) {
            System.out.println("Missing tree object for project root");
            allGood = false;
        }
    
        if (allGood) {
            System.out.println("makeTree() works");
        }
    }
    
    
    public static void generateTestFiles() throws Exception{
            new File("testFolder").mkdir();
            new File("testFolder/subFolder").mkdir();
            
            File f1 = new File("file1.txt");
            File f2 = new File("file2.txt");
            File f3 = new File("testFolder/file3.txt");
            File f4 = new File("testFolder/subFolder/file4.txt");
            
            Github.fileWriter("content one", f1);
            Github.fileWriter("content two", f2);
            Github.fileWriter("content three", f3);
            Github.fileWriter("content four", f4);
            
            System.out.println("Test files created.");
    }
}