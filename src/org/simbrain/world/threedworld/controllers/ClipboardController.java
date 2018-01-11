package org.simbrain.world.threedworld.controllers;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.world.threedworld.ThreeDWorld;
import org.simbrain.world.threedworld.ThreeDWorldComponent;
import org.simbrain.world.threedworld.engine.ThreeDEngineConverter;
import org.simbrain.world.threedworld.engine.ThreeDEngine.State;
import org.simbrain.world.threedworld.entities.BoxEntityXmlConverter;
import org.simbrain.world.threedworld.entities.Entity;
import org.simbrain.world.threedworld.entities.ModelEntityXmlConverter;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class ClipboardController implements ClipboardOwner {
    public interface ClipboardListener {
        void onClipboardChanged(ClipboardController controller);
    }

    private static boolean hasContents = false;
    private ThreeDWorld world;
    private List<ClipboardListener> listeners;

    public ClipboardController(ThreeDWorld world) {
        this.world = world;
        listeners = new ArrayList<ClipboardListener>();
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        hasContents = false;
        for (ClipboardListener listener : listeners)
            listener.onClipboardChanged(this);
    }

    public boolean hasClipboardContents() {
        return hasContents;
    }

    public void copySelection() {
        List<Entity> selection = world.getSelectionController().getSelection();
        if (!selection.isEmpty()) {
            XStream stream = ThreeDWorldComponent.getXStream();
            String xml = stream.toXML(selection);
            setClipboardContents(xml);
            hasContents = true;
        }
    }

    public void pasteSelection() {
        if (hasClipboardContents()) {
            world.getEngine().queueState(State.SystemPause, true);
            String xml = getClipboardContents();
            XStream stream = new XStream(new DomDriver());
            stream.registerConverter(new ThreeDEngineConverter(world.getEngine()));
            stream.registerConverter(new BoxEntityXmlConverter());
            stream.registerConverter(new ModelEntityXmlConverter());
            List<Entity> entities = (List<Entity>) stream.fromXML(xml);
            world.getEntities().addAll(entities);
            world.getSelectionController().selectAll(entities);
            world.getEngine().queueState(State.RunAll, false);
        }
    }

    private void setClipboardContents(String contents) {
        StringSelection selection = new StringSelection(contents);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, this);
        for (ClipboardListener listener : listeners) {
            listener.onClipboardChanged(this);
        }
    }

    private String getClipboardContents() {
        String result = "";
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        boolean hasTransferableText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        if (hasTransferableText) {
            try {
                result = (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException | IOException ex) {
                System.out.println(ex);
                ex.printStackTrace();
            }
        }
        return result;
    }

    public void addListener(ClipboardListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ClipboardListener listener) {
        listeners.remove(listener);
    }
}
