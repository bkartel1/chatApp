
package chatapplicaton;

import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXSnackbar;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.input.MouseEvent;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * FXML Controller class
 *
 * @author ahmedsalah
 */


class polling implements Runnable{

    static Socket s;
    static DataOutputStream dout = null;
    static DataInputStream din = null;
    String username;
    LobbyController lc;
    private volatile boolean exit = false;
    public polling(Socket s,DataOutputStream dout,DataInputStream din,String username,LobbyController lc) {
       this.s=s;
       this.dout=dout;
       this.din=din;
       this.username = username;
       this.lc=lc;
    }

    
    
    @Override
    public void run() {

        while(!exit)
        try {
            dout.writeUTF("request statuses");
            String[] statuses=din.readUTF().split(",");
            ObservableList<String> stats=FXCollections.observableArrayList(statuses);
            String[] groups=din.readUTF().split(",");
            ObservableList<String> grps=FXCollections.observableArrayList(groups);
            for(String str:stats){
                if ( str.startsWith(username)){
                    stats.remove(str);
                    break;
                }
            }
            Platform.runLater(new Runnable() {
            @Override public void run() {
                lc.fillusers(stats,grps);
            }
        });
            Thread.sleep(2000);
        } catch (Exception ex) {
            Logger.getLogger(polling.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void stop(){
        exit = true;
    }
    
}
class Grouppolling implements Runnable{

    static Socket s;
    static DataOutputStream dout = null;
    static DataInputStream din = null;
    String username;
    ChatRoomController lc;
    private volatile boolean exit = false;
    public Grouppolling(Socket s,DataOutputStream dout,DataInputStream din,String username,ChatRoomController lc) {
       this.s=s;
       this.dout=dout;
       this.din=din;
       this.username = username;
       this.lc=lc;
    }

    
    @Override
    public void run() {

        while(!exit)
        try {
            dout.writeUTF("room,request"+","+lc.id+","+username);
            String message=din.readUTF();
            String[] users=din.readUTF().split(",");
            Platform.runLater(new Runnable() {
            @Override 
            public void run() {
            lc.fill(message, FXCollections.observableArrayList(users));
            }
        });
            Thread.sleep(1000);
        } catch (Exception ex) {
            Logger.getLogger(polling.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void stop(){
        exit = true;
    }
    
}
public class LobbyController implements Initializable {

    String username;
     Socket s;
     DataOutputStream dout = null;
     DataInputStream din = null;
         JFXSnackbar snackbar;
     @FXML
    private JFXListView<String> groups;
    @FXML
    public JFXListView <String> users;
    
    @FXML
    private AnchorPane root;
     @FXML 
   public void handleMouseClick(MouseEvent arg0) {
    System.out.println("clickedaa on " + users.getSelectionModel().getSelectedItem());}
    
  
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
  
           //newSelection is the currently selected
           //todo

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
    public void run() {
        try{
            dout.writeUTF("close,"+username);
        }
        catch(Exception e){
            System.out.println(e.toString());
        }
    }
    
    
}));
      
     users.setOnMouseClicked(new EventHandler<MouseEvent>() {

        @Override
        public void handle(MouseEvent event) {
            System.out.println("clicked on the" + users.getSelectionModel().getSelectedItem());
         
            
       
      String[] listItem = users.getSelectionModel().getSelectedItem().split("-");
      listItem[0]=  listItem[0].trim();
      
    System.out.println("HGAAHAHAHA");
     try{
    dout.writeUTF("Get UserIp,"+listItem[0]);
    System.out.println("ONLY ONCE");
    String TargetUserIp = din.readUTF();
 
    Socket socket = new Socket(TargetUserIp,3003);
    new ChatThread(socket).start();
    
    }
    catch(Exception e)
    {e.printStackTrace();}
    
   
    }
        
    });
        groups.setOnMouseClicked(new EventHandler<MouseEvent>() {

        @Override
        public void handle(MouseEvent event) {
        int ind=groups.getSelectionModel().getSelectedIndex();
        if(ind<0) return;
            try {
                dout.writeUTF("room,add,"+ind+","+username);
                if(din.readUTF().equals("valid")){
                    Stage stage=new Stage();
                    FXMLLoader loader = new FXMLLoader();
                    loader.setLocation(getClass().getResource("ChatRoom.fxml"));
                    loader.load();
                    ChatRoomController logc = loader.getController();
                    logc.id=ind;
                    logc.name.setText(groups.getItems().get(ind));
                    logc.user=username;
                    logc.din=din;
                    logc.dout=dout;
                    logc.s=s;
                    Grouppolling gp=new Grouppolling(s, dout, din, username, logc);
                    Thread t=new Thread(gp);
                    t.start();
                    Parent root = loader.getRoot();
                    Scene scene1 = new Scene(root);
                    stage.setScene(scene1);
                    stage.show();
                    stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                    @Override
                    public void handle(WindowEvent we) {
                        gp.stop();
                        try {
                            dout.writeUTF("room,remove,"+ind+","+username);
                        } catch (IOException ex) {
                            Logger.getLogger(LobbyController.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
                    groups.getSelectionModel().clearSelection();
                }
                else {
                    snackbar=new JFXSnackbar(root);
                    snackbar.show("Can't join the room",2000);
                }
            } catch (Exception ex) {
                Logger.getLogger(LobbyController.class.getName()).log(Level.SEVERE, null, ex);
            }
}});
    }   
    public void fillusers(ObservableList<String> usr,ObservableList<String> grps){
        if(!grps.get(0).equals("")){
        groups.setItems(grps);
        groups.refresh();
        }
        users.setItems(usr);
        users.refresh();
    }
    @FXML
    void add(ActionEvent event) {
        Stage stage=new Stage();
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("groupname.fxml"));
        try {loader.load();} catch(Exception e) {
           e.printStackTrace();
          }
        GroupnameController logc = loader.getController();
        logc.lc=this;      
        Parent root = loader.getRoot();
        Scene scene1 = new Scene(root);
        stage.setScene(scene1);
        stage.show();
    }
    void newRoom(String name){
        try {
            dout.writeUTF("room,create,"+name+","+username);
        } catch (IOException ex) {
            Logger.getLogger(LobbyController.class.getName()).log(Level.SEVERE, null, ex);
        }
        Stage stage=new Stage();
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("ChatRoom.fxml"));
        try {
            loader.load();
        } catch(Exception e) {
           e.printStackTrace();
        }
        ChatRoomController logc = loader.getController();
        logc.admin=this.username;
        logc.id=groups.getItems().size();
        logc.name.setText(name);
        logc.user=username;
        logc.din=din;
        logc.dout=dout;
        logc.s=s;
        Grouppolling gp=new Grouppolling(s, dout, din, username, logc);
        Thread t=new Thread(gp);
        t.start();
        Parent root = loader.getRoot();
        Scene scene1 = new Scene(root);
        stage.setScene(scene1);
        stage.show();
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
          @Override
          public void handle(WindowEvent we) {
              gp.stop();
              try {
                            dout.writeUTF("room,remove,"+logc.id+","+username);
                        } catch (IOException ex) {
                            Logger.getLogger(LobbyController.class.getName()).log(Level.SEVERE, null, ex);
                        }
          }
      });
    }
}
