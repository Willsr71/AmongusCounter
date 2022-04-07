package sr.will.amonguscounter;

import java.io.File;

public class Arguments {
    private File patternsFolder = new File("patterns");
    private File image = new File("place.png");
    public String outputImage = "output.png";
    public byte allowedErrors = 0;

    public File getPatternsFolder() {
        return patternsFolder;
    }

    public void setPatternsFolder(String folder) {
        patternsFolder = checkFolder(folder);
    }

    public File getImage() {
        return image;
    }

    public void setImage(String file) {
        image = checkFile(file);
    }

    private File checkFile(String name) {
        File file = new File(name);
        if (!file.exists()) throw new RuntimeException("File does not exist!");
        return file;
    }

    private File checkFolder(String folder) {
        File file = checkFile(folder);
        if (!file.isDirectory()) throw new RuntimeException("Folder is not a directory!");

        return file;
    }
}
