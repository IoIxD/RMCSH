package net.minecraft.src;

import java.awt.DisplayMode;
import java.lang.reflect.Field;
import java.util.Map;
import org.lwjgl.opengl.Display;
import net.minecraft.client.Minecraft;

public class LabyrinthModNative {
    public static boolean Loaded = false;
    private static boolean TriedLoad = false;
    private static long TimeSinceTried = 0;
    private static Minecraft mc;
    private static boolean TryDrawLBMStatus = true;
    private static boolean TryDraw = true;

    private static FontRenderer Renderer;

    public static void Reset() {
        LabyrinthModNative.Loaded = false;
        LabyrinthModNative.TimeSinceTried = 0;
        LabyrinthModNative.TryDrawLBMStatus = true;
        LabyrinthModNative.TriedLoad = false;
    }

    public static void Setup(Minecraft mc) {
        if(LabyrinthModNative.TriedLoad) {
            return;
        }

        LabyrinthModNative.mc = mc;
        LabyrinthModNative.Loaded = false;
        LabyrinthModNative.TryDraw = false;

        try {
            System.out.println("Looking in "+System.getProperty("java.library.path")+" for libraries");
            System.loadLibrary("LabyrinthModNative");
            LabyrinthModNative.init();
            LabyrinthModNative.Loaded = true;
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
        try {
            fontRenderField = mcClass.getDeclaredField("fontRenderer");
        } catch(NoSuchFieldException ignored1) {
            try {
                fontRenderField = mcClass.getDeclaredField("field_6314_o");
            } catch(NoSuchFieldException ignored2) {
            }
        }
        if(fontRenderField != null) {
            try {
                LabyrinthModNative.Renderer = (FontRenderer)fontRenderField.get(mc);
            } catch(IllegalAccessException ex) {
                System.out.println("FontRenderer field not retrievable through reflection!" + ex.toString());
            }
        } else {
            System.out.println("no font renderer field found under given field names!");
            return;
        }

        LabyrinthModNative.TimeSinceTried = System.currentTimeMillis();
        LabyrinthModNative.TriedLoad = true;
        LabyrinthModNative.TryDraw = true;
    }

    public static native void init();

    public static native void setCoords(double x, double y, double z);
    public static native void setShouldReset(boolean shouldReset);

    public static void Update() {
        if(LabyrinthModNative.Loaded) {
            if(LabyrinthModNative.mc.currentScreen instanceof GuiMainMenu) {
                LabyrinthModNative.setShouldReset(true);
                LabyrinthModNative.setCoords(0, 0, 0);
            } else {
                LabyrinthModNative.setShouldReset(false);
                LabyrinthModNative.setCoords(TileEntityRenderer.staticPlayerX, TileEntityRenderer.staticPlayerY, TileEntityRenderer.staticPlayerZ);
            }
        }
    }

    public static void Draw() {
        try {
            FontRenderer renderer = LabyrinthModNative.Renderer;

            String xStr = String.valueOf((int)TileEntityRenderer.staticPlayerX);
            String yStr = String.valueOf((int)TileEntityRenderer.staticPlayerY);
            String zStr = String.valueOf((int)TileEntityRenderer.staticPlayerZ);

            if(renderer != null) {
                renderer.drawStringWithShadow("x: "+xStr, 2, 12, 16777215);
                renderer.drawStringWithShadow("y: "+yStr, 2, 22, 16777215);
                renderer.drawStringWithShadow("z: "+zStr, 2, 32, 16777215);
            }
            if(LabyrinthModNative.TryDrawLBMStatus) {
                int lbmPosition = 2;
                long timeSinceTried = (System.currentTimeMillis() - LabyrinthModNative.TimeSinceTried);
                if(timeSinceTried >= 5000) {
                    if(timeSinceTried <= 6000) {
                        lbmPosition = 2 - ((int)(timeSinceTried - 5000) / 5);
                    } else {
                        LabyrinthModNative.TryDrawLBMStatus = false;
                    }
                }
                if(LabyrinthModNative.Loaded) {
                    renderer.drawStringWithShadow("LabyrinthModNative detected", lbmPosition, 42, 43520);
                } else {
                    renderer.drawStringWithShadow("LabyrinthModNative not detected", lbmPosition, 42, 10101010);
                }
            }
        } catch(Exception ex){
            System.out.println(ex.toString());
        }
    }

}
