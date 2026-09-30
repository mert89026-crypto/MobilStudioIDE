package com.mobilstudio.ide;

import java.io.File;

public class ExplorerItem {

    private final File file;

    public ExplorerItem(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public String getName() {
        if (file == null) {
            return "";
        }

        return file.getName();
    }

    public boolean isFolder() {
        return file != null && file.isDirectory();
    }

    public boolean isFile() {
        return file != null && file.isFile();
    }

    public String getPath() {
        if (file == null) {
            return "";
        }

        return file.getAbsolutePath();
    }

    public long getSize() {
        if (file == null || !file.exists()) {
            return 0;
        }

        return file.length();
    }

    public long getLastModified() {
        if (file == null || !file.exists()) {
            return 0;
        }

        return file.lastModified();
    }

    public boolean isHidden() {
        return file != null && file.isHidden();
    }

    public String getExtension() {

        String name = getName();

        int dotIndex =
                name.lastIndexOf('.');

        if (dotIndex <= 0 ||
                dotIndex >= name.length() - 1) {

            return "";
        }

        return name.substring(
                dotIndex + 1
        ).toLowerCase();
    }

    public boolean isJavaFile() {
        return getExtension().equals("java");
    }

    public boolean isKotlinFile() {
        return getExtension().equals("kt");
    }

    public boolean isXmlFile() {
        return getExtension().equals("xml");
    }

    public boolean isGradleFile() {

        String name =
                getName().toLowerCase();

        return name.equals("build.gradle")
                || name.equals("settings.gradle")
                || name.endsWith(".gradle");
    }

    public boolean isJsonFile() {
        return getExtension().equals("json");
    }

    public boolean isTextFile() {

        String extension =
                getExtension();

        return extension.equals("txt")
                || extension.equals("md")
                || extension.equals("properties")
                || extension.equals("pro")
                || extension.equals("xml")
                || extension.equals("java")
                || extension.equals("kt")
                || extension.equals("gradle")
                || extension.equals("json");
    }

    public String getDisplayType() {

        if (isFolder()) {
            return "Klasör";
        }

        if (isJavaFile()) {
            return "Java";
        }

        if (isKotlinFile()) {
            return "Kotlin";
        }

        if (isXmlFile()) {
            return "XML";
        }

        if (isGradleFile()) {
            return "Gradle";
        }

        if (isJsonFile()) {
            return "JSON";
        }

        if (isTextFile()) {
            return "Metin";
        }

        return "Dosya";
    }

    public String getIcon() {

        if (isFolder()) {
            return "📁";
        }

        if (isJavaFile()) {
            return "☕";
        }

        if (isKotlinFile()) {
            return "K";
        }

        if (isXmlFile()) {
            return "📄";
        }

        if (isGradleFile()) {
            return "⚙";
        }

        if (isJsonFile()) {
            return "{}";
        }

        if (isTextFile()) {
            return "📝";
        }

        String extension =
                getExtension();

        if (extension.equals("png")
                || extension.equals("jpg")
                || extension.equals("jpeg")
                || extension.equals("webp")
                || extension.equals("gif")) {

            return "🖼";
        }

        if (extension.equals("aar")
                || extension.equals("jar")) {

            return "📦";
        }

        return "📄";
    }

    @Override
    public String toString() {

        return getName();
    }
}
