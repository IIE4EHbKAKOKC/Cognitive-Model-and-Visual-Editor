package cognitive_system.file_integration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import fsm.*;
import cognitive_system.*;
import visual_editor.VisualiseInfo;
import visual_editor.GeometryInfo;

public class FileReader {
    private Scanner sc;
    private String mode;
    private AutomatReader ar;
    private AutomatLayerReader alr;
    private CognitiveSystemReader csr;
    private String parcer;
    private HashMap <String, Automat> automats;
    private HashMap <String, AutomatLayer> layers;
    private HashMap <String, CognitiveSystem> cSystems;
    private ArrayList <VisualiseInfo> vInfos;
    public FileReader (String way, String parcer) throws Exception{
        sc = new Scanner(new File(way));
        mode = "default";
        this.parcer = parcer;//not "/", not " "
        automats = new HashMap<String, Automat>();
        layers = new HashMap<String, AutomatLayer>();
        cSystems = new HashMap <String, CognitiveSystem>();
        vInfos = new ArrayList<VisualiseInfo>();
        read();
        //System.out.println(automats);
        //System.out.println(layers);
        //System.out.println(cSystems);
    }
    private void read() {
        while(sc.hasNextLine()) 
            update(sc.nextLine());
    }
    private void update (String s) {
        String[] data = s.split(parcer);
        switch (data[0]) {
            case "automat":
                if (mode == "default") {
                    mode = "automat";
                    if (data.length == 3) {
                        ar = new AutomatReader(data[1],data[2]);
                        //обычный автомат
                    }
                    else if ((data.length == 5)&&(data[3].equals("sleeping"))) {
                        ar = new AutomatReader(data[1],data[2],data[4]);
                    }
                }
                break;
            case "state":
                if (mode == "automat") {
                    ar.readedState(data);
                }
                break;
            case "transition":
                if (mode == "automat") {
                    ar.readedTransition(data);
                }
                break;
            case "/automat":
                if (mode == "automat") {
                    Automat a = ar.getAutomat();
                    automats.put(a.name, a);
                    mode = "default";
                }
                break;
            case "layer":
                if (mode == "default") {
                    mode = "layer";
                    alr = new AutomatLayerReader(data[1]);
                }
                break;
            case "add":
                if ((mode == "layer")&&(data[1].equals("automat"))) {
                    try {
                        alr.addAutomat(automats.get(data[2]));
                    }
                    catch (Exception e) {
                        //System.out.println(e);
                    }
                }
                else if ((mode=="cognitive")&&(data[1].equals("layer"))) {
                    try {
                        csr.addLayer(layers.get(data[2]));
                    }
                    catch (Exception e) {
                        //System.out.println(e);
                    }
                }
                break;
            case "/layer":
                if (mode == "layer") {
                    mode = "default";
                    layers.put(alr.getAutomatLayer().id, alr.getAutomatLayer());
                }
                break;
            case "cognitive":
                if (mode == "default") {
                    mode = "cognitive";
                    csr = new CognitiveSystemReader(data[1]);
                }
                break;
            case "/cognitive":
                cSystems.put(csr.getCognitiveSystem().id, csr.getCognitiveSystem());
                break;
            case "visual":
                GeometryInfo gi;
                if (data.length == 8) {
                    gi = new GeometryInfo(Integer.parseInt(data[4]),Integer.parseInt(data[5]), Integer.parseInt(data[6]), Integer.parseInt(data[7]));
                }
                else {
                    gi = getGeometryInfo(data[1],data[3]);
                }
                
                vInfos.add(new VisualiseInfo(data[1], data[2], gi, data[3]));
            break;
        }
    } 
    private GeometryInfo getGeometryInfo (String name, String parentName) {
        int i = name.indexOf("to");
        String[] parents = parentName.split("/");
        //String from = name.substring(0, i);
        String from = parents[1];
        String parentAutomat = parents[0];
        String to = name.substring(i+2, name.length());
        GeometryInfo fromInfo = new GeometryInfo(0, 0, 0, 0);
        GeometryInfo toInfo = new GeometryInfo(0, 0, 0, 0);
        for (VisualiseInfo vi:vInfos) {
            if (vi.getElementType().equals("state")&&(vi.getParentName().equals(parentAutomat))) {
                if (vi.getElementName().equals(from))
                    fromInfo = vi.getGeometryInfo();
                else if (vi.getElementName().equals(to)) 
                    toInfo = vi.getGeometryInfo();
            }
        }
        return new GeometryInfo(
            fromInfo.getX() + Math.round(fromInfo.getX1()/2),
            fromInfo.getY() + Math.round(fromInfo.getY1()/2), 
            toInfo.getX() + Math.round(toInfo.getX1()/2),
            toInfo.getY() + Math.round(toInfo.getY1()/2)
        );
    }
    public HashMap <String, CognitiveSystem> getCognitiveSystems () {
        return this.cSystems;
    }
    public HashMap <String, Automat> getAutomats () {
        return this.automats;
    }
    public ArrayList <VisualiseInfo> getVisualiseInfos () {
        return this.vInfos;
    }
}

class AutomatReader {
    private String name;
    private int dt;
    private HashMap<String, State> states;
    private Transitions transitions;
    private String layerPrefix;

    public AutomatReader(String name, String dt) {
        this.name = name;
        this.layerPrefix = "";
        states = new HashMap<String, State>();
        transitions = new Transitions();
        this.dt = Integer.parseInt(dt);
        //System.out.println("new AutomatReader");
    }
    public AutomatReader(String name, String dt, String prefix) {
        this.name = name;
        this.layerPrefix = prefix;
        states = new HashMap<String, State>();
        transitions = new Transitions();
        this.dt = Integer.parseInt(dt);
        //System.out.println("new SleepingAutomatReader");
    }
    public void readedState (String[] data) {
        if (data[1].equals("simple")) {
            if (data.length == 4) {
                //System.out.println("adding simple state");
                states.put(data[2], new SimpleState(data[2],data[3]));
            }
        }
    }
    public void readedTransition (String[] data) {
        //System.out.println("readedTransition");
        if ((data.length > 4)&&(!data[4].equals(""))) {
            String doingString = "";
            if ((data.length>5)&&(!data[5].equals(""))) {
                //managingTransition
                ////System.out.println("managingTransition");
                if (data.length == 7) doingString = data[6];
                transitions.addTransition(new ManagingTransition(data[1], data[2], data[3], data[4], doingString, data[5]));
            }
            else {
                //simpleTransition
                ////System.out.println("simpleTransition");
                if (data.length == 7) doingString = data[6];
                transitions.addTransition(new SimpleTransition(data[1], data[2], data[3], data[4], doingString));
            }
        }
        else {//all
            if ((data.length>5)&&(!data[5].equals(""))) {
                //allManagingTransition
                ////System.out.println("allManagingTransition");
                transitions.addTransition(new AllManagingTransition(data[1], data[2], data[3], data[5]));
            }
            else {
                //allTransition
                ////System.out.println("allTransition");
                transitions.addTransition(new AllTransition(data[1], data[2], data[3]));
            }
        }
    }
    public Automat getAutomat() {
        if (layerPrefix.equals("")) {
            return new Automat(states, transitions.getHashMap(), dt, name);
        }
        else {
            return new SleepingAutomat(states, transitions.getHashMap(), dt, name, layerPrefix);
        }
    }
}

class AutomatLayerReader {
    private AutomatLayer al;
    public AutomatLayerReader (String id) {
        al = new AutomatLayer(id);
    }
    public void addAutomat(Automat a) {
        al.addAutomat(a);
    }
    public AutomatLayer getAutomatLayer () {
        return this.al;
    }
}

class CognitiveSystemReader {
    private CognitiveSystem cs;
    public CognitiveSystemReader (String id) {
        cs = new CognitiveSystem(id);
    }
    public void addLayer (AutomatLayer al) {
        cs.addLayer(al);
    }
    public CognitiveSystem getCognitiveSystem () {
        return this.cs;
    } 
}