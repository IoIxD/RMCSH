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
import java.io.PrintWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;

public class RMCSHNative {
    public static boolean ShouldSave = false;
    public static boolean Loaded = false;
    private static boolean TriedLoad = false;
    private static long TimeSinceTried = 0;
    private static Minecraft mc;
    private static boolean TryDrawLBMStatus = true;
    private static boolean TryDraw = true;
    private static String GameTitle;
    private static File optionsFile;

    private static FontRenderer Renderer;

    private static List<String> PossibleFontRendererFieldNames = Arrays.asList("fontRenderer","field_6314_o","o");

    enum OSEnum {
            linux,
            solaris,
            windows,
            macos,
            unknown;
    }

    static {
        RMCSHNative.optionsFile = new File(RMCSHNative.getAppDir("minecraft"), "rmcsh_options.txt");
        boolean foundAnyOptions = false;
        try {
            RMCSHNative.optionsFile.createNewFile();
            System.out.println(RMCSHNative.optionsFile.getPath());
			if(RMCSHNative.optionsFile.exists()) {
    			BufferedReader var1 = new BufferedReader(new FileReader(RMCSHNative.optionsFile));
    			String var2 = "";

    			while(true) {
    				var2 = var1.readLine();
    				if(var2 == null) {
    					var1.close();
    					break;
    				}

    				try {
    					String[] var3 = var2.split(":");
    					if(var3[0].equals("should_save")) {
    						RMCSHNative.ShouldSave = Boolean.valueOf(var3[1].replace(" ",""));
                            System.out.println("[RCMSH] should_save = " + String.valueOf(RMCSHNative.ShouldSave) + "(" + var3[1] + ")");
     					    foundAnyOptions = true;
    					}
    				} catch (Exception var5) {
    					System.out.println("Skipping bad option: " + var2);
    				}
    			}
    			if(!foundAnyOptions) {
    			    PrintWriter options = new PrintWriter(new FileWriter(RMCSHNative.optionsFile));
    				options.println("should_save: true");
    				options.close();
    			}
			}
		} catch (Exception var6) {
			System.out.println("Failed to load options");
			var6.printStackTrace();
		}
    }

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


        System.out.println("Saving: " + String.valueOf(RMCSHNative.ShouldSave));

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
            } catch (Exception ex) {
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

    private static OSEnum getOs() {
            String var0 = System.getProperty("os.name").toLowerCase();
            return var0.contains("win") ? OSEnum.windows : (var0.contains("mac") ? OSEnum.macos : (var0.contains("solaris") ? OSEnum.solaris : (var0.contains("sunos") ? OSEnum.solaris : (var0.contains("linux") ? OSEnum.linux : (var0.contains("unix") ? OSEnum.linux : OSEnum.unknown)))));
    }


    private static File getAppDir(String var0) {
            String var1 = System.getProperty("user.home", ".");
            File var2;
            int os = getOs().ordinal();
            switch(os) {
            case 1:
            case 2:
                    var2 = new File(var1, '.' + var0 + '/');
                    break;
            case 3:
                    String var3 = System.getenv("APPDATA");
                    if(var3 != null) {
                            var2 = new File(var3, "." + var0 + '/');
                    } else {
                            var2 = new File(var1, '.' + var0 + '/');
                    }
                    break;
            case 4:
                    var2 = new File(var1, "Library/Application Support/" + var0);
                    break;
            default:
                    var2 = new File(var1, var0 + '/');
            }

            if(!var2.exists() && !var2.mkdirs()) {
                    throw new RuntimeException("The working directory could not be created: " + var2);
            } else {
                    return var2;
            }
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
