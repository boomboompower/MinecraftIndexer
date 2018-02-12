package me.boomboompower.mcassets;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;

public class Indexer {

    private final Scanner scanner = new Scanner(System.in);
    private File assetsDir;

    private int successes = 0;

    private Indexer() {
    }

    public static void main(String[] args) {
        new Indexer().begin(new File("."));
    }

    private void begin(File assetsDir) {
        this.assetsDir = assetsDir;

        List<File> indexDir = listFiles(assetsDir, f -> f.isDirectory() && f.getName().equals("indexes"));
        if (indexDir.size() > 0) {
            beginIndexLogic(listFiles(indexDir.get(0), f -> f.isFile() && f.getName().endsWith(".json")));
        } else {
            log("No \"indexes\" directory was found in the running directory " + assetsDir.getAbsolutePath());
            System.exit(-1);
        }
    }

    private void beginIndexLogic(List<File> files) {
        if (files.isEmpty()) {
            System.out.print("No index files found, please ensure you are running this in the correct directory");
            System.exit(0);
            return;
        }

        StringBuilder toLog = new StringBuilder("Please select which version you would like to grab the assets from: \n");

        for (File index : files) {
            toLog.append("  - ").append(index.getName().replace(".json", "")).append("\n");
        }

        log(toLog.toString().trim() + "\n");
        String version = this.scanner.next();
        boolean flagged = false;

        if (getFrom(version, files) != null) {
            flagged = true;
        }

        if (!flagged) {
            log(" \nThe version \"" + version + "\" was not found\n");
            beginIndexLogic(files);
        } else {
            log("Grabbing assets for version: " + version);

            File indexJson = getFrom(version, files);

            if (indexJson == null) {
                log(" \nAn error occured whilst grabbing the necessary information, Please retry!\n");
                beginIndexLogic(files);
            } else {
                realIndexLogic(version, indexJson);

                log(" \n" +
                        "Process finished with " + this.successes + " file(s) successfully copied!\n" +
                        "All copied resources are located in the generated " + version + " directory"
                );

            }
        }
    }

    private void realIndexLogic(String version, File indexJsonFile) {
        createIndexDir(version);

        BetterJsonObject json = new BetterJsonObject(indexJsonFile);

        if (!json.has("objects")) {
            System.exit(-1);
            return;
        }

        json = new BetterJsonObject(json.get("objects").getAsJsonObject());

        for (Map.Entry<String, JsonElement> entry : json.getData().entrySet()) {
            JsonObject entryObj = entry.getValue().getAsJsonObject();

            if (!entryObj.has("hash")) {
                continue;
            }

            String location = entry.getKey();
            String hash = entryObj.get("hash").getAsString();

            File objects = new File(this.assetsDir, "objects");

            if (!objects.exists() || objects.isFile()) {
                log("Invalid objects location, does it exist?");
                System.exit(-1);
                return;
            }

            File hashedFile = new File(objects, hash.substring(0, 2) + File.separator + hash);
            File copyFile = new File(this.assetsDir, "generated" + File.separator + version + File.separator + location.replace("/", File.separator));

            if (!hashedFile.exists()) {
                System.err.println("Could not find file at " + hashedFile.getAbsolutePath());
                continue;
            }

            try {
                FileUtils.copyFile(hashedFile, copyFile);
                log("Successfully created file at " + location);

                this.successes++;
            } catch (IOException e) {
                System.err.println("Failed to copy file " + location);
                e.printStackTrace();
            }
        }
    }

    private void log(String message) {
        if (message.contains("\n")) {
            String[] split = message.split("\n");
            for (String s : split) {
                System.out.println(s);
            }
        } else {
            System.out.println(message);
        }

        if (message.endsWith("\n")) {
            System.out.println();
        }
    }

    private void createIndexDir(String name) {
        File generatedMain = new File(this.assetsDir, "generated" + File.separator + name);

        if (generatedMain.exists()) {
            FileUtils.deleteQuietly(generatedMain);
        }

        generatedMain.mkdirs();
    }

    private static File getFrom(String input, List<File> files) {
        for (File file : files) {
            if (file.getName().replace(".json", "").equalsIgnoreCase(input)) {
                return file;
            }
        }
        return null;
    }

    private static List<File> listFiles(File in, FileFilter filter) {
        if (in == null) {
            return new ArrayList<>();
        } else if (filter == null) {
            try {
                return Arrays.asList(Objects.requireNonNull(in.listFiles()));
            } catch (NullPointerException ex) {
                return new ArrayList<>();
            }
        } else {
            try {
                return Arrays.asList(Objects.requireNonNull(in.listFiles(filter)));
            } catch (NullPointerException ex) {
                return new ArrayList<>();
            }
        }
    }
}
