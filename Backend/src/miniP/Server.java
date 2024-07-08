package miniP;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    public static void main(String[] args) throws IOException {
        ServerSocket ss8080 = new ServerSocket(8080);
        ServerSocket ss4050 = new ServerSocket(4050);
//        while (true) {
//            System.out.println("The server is starting ...");
//            new ClientHandler(ss8080.accept()).start();
//        }
//        System.out.println("The server is starting ...");

        new Thread(() -> {

            try{
                while (true) {
                    new ClientHandler(ss8080.accept()).start();
                    System.out.println("server on port 8080 is starting ...");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();


        new Thread(() -> {

            try{
                while (true) {
                    new ClientHandler2(ss4050.accept()).start();
                    System.out.println("server on port 4050 is starting ...");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}




class ClientHandler extends Thread{

    DataOutputStream dos;
    DataInputStream dis;
    Socket socket;

    ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        super.run();

        try {


            String query = "";

            int index = dis.read();

            while (index != 0){
                query += (char)index;
                index = dis.read();
            }
//            query = dis.readUTF();
            System.out.println(query);
            String[] queryArr = query.split("-");
            System.out.println("the query is : " + query);


            switch (queryArr[0]){
                case "LoginChecker":
                    boolean usernameC = false;
                    boolean passwordC = false;

                    ArrayList<Student> students = Admin.retrieveData(Student.class);

                    for(Student student: students){
//                        System.out.println(student.getUsername());
                        if(student.getUsername().equals(queryArr[1])){

                            usernameC = true;
                            if(student.getPassword().equals(queryArr[2])){
                                passwordC = true;
                            }
                        }
                    }

                    if(!usernameC){
                        dos.writeBytes("404");
                        dos.flush();
                        System.out.println("404");
                    }
                    else if(usernameC & !passwordC){
                        dos.writeBytes("401");
                        dos.flush();
                        System.out.println("401");
                    }
                    else if(usernameC & passwordC){
                        dos.writeBytes("200");
                        dos.flush();
                        System.out.println("200");
                    }

                    dos.close();
                    dis.close();

                    break;

                case "getUserInfo" :
                    students = Admin.retrieveData(Student.class);
                    String sid = queryArr[1];
                    String info = "";

                    for(Student st : students){
                        if(st.getSID().equals(sid)){
                            info = st.getStudentName() + "-دانشجو-" + st.getSID() + "-" +st.getCurrentTerm() + '-' + st.getNumberOfUnits2() + '-' + st.getAverageScore2()  + '-' + st.getImage();
                        }
                    }
//                    System.out.println("the info is : " + info);
                    dos.writeUTF(info);
//                    dos.writeBytes(info);
                    dos.flush();
                    dos.close();
                    dis.close();
                    break;


                case "changePassword":
                    students = Admin.retrieveData(Student.class);
                    sid = queryArr[1];
                    String passwordUserR = queryArr[2];
                    String newPasswordUserR = queryArr[3];
                    System.out.println(sid + "-" + passwordUserR + "-" + newPasswordUserR);


                    for(Student s : students){
                        if(s.getSID().equals(sid)){
                            if(s.getPassword().equals(passwordUserR)){
                                if(Cli.passwordChecking(newPasswordUserR, s.getUsername())){
                                    s.setPassword(newPasswordUserR);
                                    dos.writeBytes("200"); //password changed successfully
                                    dos.flush();
                                    dos.close();
                                    System.out.println("200");
                                }else{
                                    dos.writeBytes("402");// new password is weak
                                    dos.flush();
                                    dos.close();
                                    System.out.println("402");
                                }
                            }else{
                                dos.writeBytes("401");// old password is wrong
                                dos.flush();
                                dos.close();
                                System.out.println("401");
                            }
                        }
                    }

                    dis.close();

                    break;


                case "removeAccount":
                    sid = queryArr[1];
                    students = Admin.retrieveData(Student.class);
                    boolean res = false;

                    for (Student s: students){
                        if(s.getSID().equals(sid)){
                            res = Admin.removeData(s);
                        }
                    }
                    if(res) {
                        dos.writeBytes("200");
                        dos.close();
                        System.out.println("200");
                    }else{
                        dos.writeBytes("401");// failed to remove
                        dos.close();
                        System.out.println("401");
                    }

                    dis.close();

                    break;


                case "changeFields":

                    System.out.println("I'm here ch");

                    students = Admin.retrieveData(Student.class);

                    Student student = null;

                    Class<?> clazz = Student.class;
                    Field[] fields = clazz.getDeclaredFields();



                    String filed = queryArr[1];
                    sid = queryArr[2];
                    String newValue = queryArr[3];

                    for(Student st : students){
                        if(st.getSID().equals(sid)){
                            student = st;
                            break;
                        }
                    }

                    for(Field spField : fields){
                        if(spField.getName().equals(filed)){
                            Admin.removeData(student);
                            String setterName = "set" + Character.toUpperCase(filed.charAt(0)) + filed.substring(1);
                            Method setterMethod  = clazz.getMethod(setterName, spField.getType());
                            setterMethod.invoke(student, newValue);
                            Admin.addData(student);
                        }
                    }

                    dos.writeBytes("200");
                    dos.close();
                    System.out.println("200");
                    break;




            }
        } catch (IOException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {

            try {

                if(dis != null)  dis.close();
                if(dos != null) dos.close();
                if(socket != null) socket.close();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }
}
class ClientHandler2 extends Thread{

    DataOutputStream dos;
    DataInputStream dis;
    Socket socket;

    ClientHandler2(Socket socket) throws IOException {
        this.socket = socket;
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        super.run();

        try {


//            String query = "";

            byte[] bytes = new byte[1024]; // Adjust the size as needed

            int length = dis.read(bytes);
            String query = new String(bytes, 0, length, "UTF-8");
            query = query.substring(0, query.length()-1);

            System.out.println("Received query: " + query);
            String[] queryArr = query.split("-");

//            int index = dis.read();
//
//            while (index != 0){
//                query += (char)index;
//                index = dis.read();
//            }
//            query = dis.readUTF();
//            System.out.println(query);
//            String[] queryArr = query.split("-");
//            System.out.println("the query is : " + query);


            switch (queryArr[0]){

                case "changeFields":

                    System.out.println("I'm here ch");

                    ArrayList<Student> students = Admin.retrieveData(Student.class);

                    Student student = null;

                    Class<?> clazz = Student.class;
                    Field[] fields = clazz.getDeclaredFields();



                    String filed = queryArr[1];
                    String sid = queryArr[2];
                    String newValue = queryArr[3];

                    for(Student st : students){
                        if(st.getSID().equals(sid)){
                            student = st;
                            break;
                        }
                    }



                    for(Field spField : fields){
                        if(spField.getName().equals(filed)){
                            Admin.removeData(student);
                            String setterName = "set" + Character.toUpperCase(filed.charAt(0)) + filed.substring(1);
                            Method setterMethod  = clazz.getMethod(setterName, spField.getType());
                            if(spField.getType() == int.class){
                                int val = Integer.parseInt(newValue);
                                setterMethod.invoke(student, val);
                            }else if(spField.getType() == double.class){
                                double val = Double.parseDouble(newValue);
                                setterMethod.invoke(student, val);
                            }else{
                                setterMethod.invoke(student, newValue);
                            }
                            Admin.addData(student);
                        }
                    }

                    dos.writeBytes("200");
                    dos.close();
                    System.out.println("200");
                    break;




            }
        } catch (IOException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {

            try {

                if(dis != null)  dis.close();
                if(dos != null) dos.close();
                if(socket != null) socket.close();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }
}
