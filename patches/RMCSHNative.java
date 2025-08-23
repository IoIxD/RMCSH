package net.minecraft.src;

import java.awt.DisplayMode;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import org.lwjgl.opengl.Display;
import net.minecraft.client.Minecraft;

public class RMCSHNative {
    public static boolean Loaded = false;
    private static boolean TriedLoad = false;
    private static long TimeSinceTried = 0;
    private static Minecraft mc;
    private static boolean TryDrawLBMStatus = true;
    private static boolean TryDraw = true;
    private static String GameTitle;

    private static FontRenderer Renderer;

    private static List<String> PossibleFontRendererFieldNames = Arrays.asList("fontRenderer","field_6314_o","o");

    public static void Reset() {
        RMCSHNative.Loaded = false;
        RMCSHNative.TimeSinceTried = 0;
        RMCSHNative.TryDrawLBMStatus = true;
        RMCSHNative.TriedLoad = false;
    }

    public static void Setup(Minecraft mc) {
        if(RMCSHNative.TriedLoad) {
            return;
        }

        RMCSHNative.mc = mc;
        RMCSHNative.Loaded = false;
        RMCSHNative.TryDraw = false;

        RMCSHNative.GameTitle = Display.getTitle();

        // Try and extract RMCSHNative.dll or libRMCSHNative.so from within the .jar
        String filename = "";
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            filename = "RMCSHNative.dll";
        } else if (os.contains("linux")) {
            filename = "libRMCSHNative.so";
        } else {
            System.out.println("Unsupported operating system \"" + os + "\". Refusing to load native mod.");
        }

        String[] paths = System.getProperty("java.library.path").split(":");
        for(String path : paths) {
            InputStream link = (RMCSHNative.class.getResourceAsStream("/" + filename));
            try {
                Files.copy(link, new File(path + "/" + filename).toPath());
                System.out.println("Extracted native library to "+path);
            } catch (IOException ex) {
                System.out.println("Could not extract native library to "+path+"! "+ex.toString());
            }
        }

        try {
            System.out.println("Looking in "+System.getProperty("java.library.path")+" for libraries");
            System.loadLibrary("RMCSHNative");
            RMCSHNative.init();
            RMCSHNative.Loaded = true;
        } catch(UnsatisfiedLinkError ex) {
            System.out.println("Could not load native library! "+ex.toString());
        }

        // We use reflection next to get the proper values, as some versions have them obfuscated and some of them do not.
        Class mcClass = null;
        try {
            mcClass = Class.forName("net.minecraft.client.Minecraft");
        } catch(ClassNotFoundException ex) {
            System.out.println("Minecraft class not found through reflection!" + ex.toString());
            return;
        }

        Field fontRenderField = null;
        for(String name : RMCSHNative.PossibleFontRendererFieldNames) {
            try {
                fontRenderField = mcClass.getDeclaredField(name);
                break;
            } catch(NoSuchFieldException ignored) {
            }
        }
       
        if(fontRenderField != null) {
            try {
                RMCSHNative.Renderer = (FontRenderer)fontRenderField.get(mc);
            } catch(IllegalAccessException ex) {
                System.out.println("FontRenderer field not retrievable through reflection!" + ex.toString());
            }
        } else {
            System.out.println("no font renderer field found under given field names!");
            System.out.print("Avaliable fields: ");
            for(Field f : mcClass.getDeclaredFields()) {
                System.out.print(f.getName()+" ");
            }
            System.out.print("\n");
            return;
        }

        RMCSHNative.TimeSinceTried = System.currentTimeMillis();
        RMCSHNative.TriedLoad = true;
        RMCSHNative.TryDraw = true;
    }

    public static native void init();

    public static native void setCoords(double x, double y, double z);
    public static native void setShouldReset(boolean shouldReset);
    public static native void setTitle(String title);

    public static void Update() {
        if(RMCSHNative.Loaded) {
            if(RMCSHNative.mc.currentScreen instanceof GuiMainMenu) {
                RMCSHNative.setShouldReset(true);
                RMCSHNative.setCoords(0, 0, 0);
            } else {
                RMCSHNative.setShouldReset(false);
                RMCSHNative.setCoords(TileEntityRenderer.staticPlayerX, TileEntityRenderer.staticPlayerY, TileEntityRenderer.staticPlayerZ);
            }
            if(RMCSHNative.GameTitle == "") {
                RMCSHNative.GameTitle = Display.getTitle();
            }
            RMCSHNative.setTitle(RMCSHNative.GameTitle);
        }
    }

    public static void Draw() {
        try {
            FontRenderer renderer = RMCSHNative.Renderer;

            String xStr = String.valueOf((int)TileEntityRenderer.staticPlayerX);
            String yStr = String.valueOf((int)TileEntityRenderer.staticPlayerY);
            String zStr = String.valueOf((int)TileEntityRenderer.staticPlayerZ);

            if(renderer != null) {
                renderer.drawStringWithShadow("x: "+xStr, 2, 12, 16777215);
                renderer.drawStringWithShadow("y: "+yStr, 2, 22, 16777215);
                renderer.drawStringWithShadow("z: "+zStr, 2, 32, 16777215);
            }
            if(RMCSHNative.TryDrawLBMStatus) {
                int lbmPosition = 2;
                long timeSinceTried = (System.currentTimeMillis() - RMCSHNative.TimeSinceTried);
                if(timeSinceTried >= 5000) {
                    if(timeSinceTried <= 6000) {
                        lbmPosition = 2 - ((int)(timeSinceTried - 5000) / 5);
                    } else {
                        RMCSHNative.TryDrawLBMStatus = false;
                    }
                }
                if(RMCSHNative.Loaded) {
                    // renderer.drawStringWithShadow("RMCSHNative detected", lbmPosition, 42, 43520);
                } else {
                    renderer.drawStringWithShadow("RMCSHNative not detected", lbmPosition, 42, 10101010);
                }
            }
        } catch(Exception ex){
            System.out.println(ex.toString());
        }
    }

}
