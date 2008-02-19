package org.simbrain.workspace;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.simbrain.workspace.gui.DesktopComponent;
import org.simbrain.workspace.gui.SimbrainDesktop;


public class WorkspaceSerializer {
    private final Workspace workspace;
    
    public WorkspaceSerializer(Workspace workspace) {
        this.workspace = workspace;
    }
    
    public void exportWorkspace() {
        
    }
    
    public void importWorkspace() {
        
    }
    
    public void writeWorkspace(File file)
    {
        
    }
    
    public void serialize(OutputStream output) throws IOException {
        ZipOutputStream zipStream = new ZipOutputStream(output);
        
//        zipStream.setMethod(ZipOutputStream.STORED);
        
        WorkspaceComponentSerializer serializer = new WorkspaceComponentSerializer();
        ArchiveContents archive = new ArchiveContents(serializer);

        for (Coupling<?> coupling : workspace.getManager().getCouplings()) {
            archive.addCoupling(coupling);
        }
        
        for (WorkspaceComponent<?> component : workspace.getComponentList()) {
            ArchiveContents.Component archiveComp = archive.addComponent(component);
            
            ZipEntry entry = new ZipEntry(archiveComp.uri);
            zipStream.putNextEntry(entry);
            serializer.serializeComponent(component, zipStream);
        }
        
        ZipEntry entry = new ZipEntry("contents.xml");
        zipStream.putNextEntry(entry);
        archive.toXml(zipStream);
        
        zipStream.finish();
    }
    
    public void deserialize(InputStream stream) throws IOException {
        Map<String, byte[]> entries = new HashMap<String, byte[]>();
        
        CachingInputStream cachedStream = new CachingInputStream(stream);
        
        ZipInputStream zip = new ZipInputStream(cachedStream);
        
        try {
            ArchiveContents contents = null;
            WorkspaceComponentDeserializer componentDeserializer = new WorkspaceComponentDeserializer();
            
            System.out.println("deserializing");
            
            ZipEntry entry = zip.getNextEntry();
            
            for (ZipEntry next; entry != null; entry = next) {
                next = zip.getNextEntry();
                
                String name = entry.getName();
                System.out.println("entry: " + name);
                System.out.println("entry.getSize(): " + entry.getSize());
                
                byte[] last = cachedStream.getCached();
                
                System.out.println("bytes: " + last.length);
                
                if ("contents.xml".equals(name)) {
                    contents = (ArchiveContents) ArchiveContents.xstream().fromXML(
                        new ByteArrayInputStream(last));
//                        new SubsetInputStream(zip, entry.getSize()));
                    
    //                System.out.println("entry.getSize(): " + entry.getSize());
    //                for (TableOfContents.Component component : contents.components) {
    //                    System.out.println("component: " + component.uri);
    //                }
                } else {
//                    byte[] bytes = new byte[(int) entry.getSize()];
                    
//                    int pos = 0;
//                    int read;
//                    
//                    do {
//                        read = zip.read(bytes, pos, bytes.length - pos);
//                        System.out.println("read: " + read);
//                        
//                        if (read < 0) break;
//                        pos += read;
//                    } while (pos < bytes.length);
//                    
//                    if (pos != bytes.length) {
//                        throw new IllegalArgumentException("did not get full entry");
//                    }
                    
                    entries.put(entry.getName(), last);
                }
            }
            
            for (ArchiveContents.Component component : contents.components) {
                WorkspaceComponent<?> wc = componentDeserializer.deserializeWorkspaceComponent(
                    component.className, new ByteArrayInputStream(
                    entries.get(component.uri)), "name", null);

//                if (component.desktopComponent != null) {
//                    workspace.toggleEvents(false);
//                    
//                    DesktopComponent dc = componentDeserializer.deserializeDesktopComponent(
//                        component.desktopComponent.className, new ByteArrayInputStream(
//                        entries.get(component.desktopComponent.uri)), "name");
//                    
//                    SimbrainDesktop.getDesktop(workspace).addComponent(wc, dc);
//                    
//                    workspace.toggleEvents(true);
//                }
                
                workspace.addWorkspaceComponent(wc);
            }
            
            for (ArchiveContents.Coupling coupling : contents.couplings) {
                workspace.addCoupling(new Coupling(
                    (ProducingAttribute<?>) componentDeserializer.getAttribute(coupling.source),
                    (ConsumingAttribute<?>) componentDeserializer.getAttribute(coupling.target)));
            }
        } finally {
            zip.close();
        }
    }
    
    private class SubsetInputStream extends InputStream {
        int index = 0;
        final InputStream stream;
        final long length;
        
        SubsetInputStream(InputStream stream, long length) {
            this.stream = stream;
            this.length = length;
        }

        @Override
        public int read() throws IOException {
            if (index++ < length) return stream.read();
            else return -1;
        }
    }
    
    class CachingInputStream extends InputStream {
        private final InputStream wrapped;
        private final ByteArrayOutputStream cache = new ByteArrayOutputStream();
        
        CachingInputStream(InputStream istream) {
            this.wrapped = istream;
        }
        
        public int available() throws IOException {
            return wrapped.available();
        }
        
        public void close() throws IOException {
            wrapped.close();
        }
        
        public int read() throws IOException {
            int b = wrapped.read();
            
            cache.write(b);
            
            return wrapped.read();
        }
        
        public int read(byte[] b, int off, int len) throws IOException {
            byte[] bytes = new byte[len];
            
            int read = wrapped.read(bytes);
            cache.write(bytes, 0, read);
            
            System.arraycopy(bytes, 0, b, off, len);
            
            return read;
        }
        
        public long skip(long n) throws IOException {
            int len = (int) n;
            
            byte[] bytes = new byte[len];
            
            int read = wrapped.read(bytes);
            cache.write(bytes, 0, read);
            
            return read;
        }
        
        byte[] getCached() {
            byte[] bytes = cache.toByteArray();
            
            return bytes;
        }
    }
    
//    class Zip {
//        final ZipInputStream stream;
//        
//        Zip(InputStream istream) {
//            stream = new ZipInputStream(istream);
//        }
//    }
}
