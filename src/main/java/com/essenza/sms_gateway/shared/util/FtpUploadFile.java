//package com.essenza.sms_gateway.shared.util;
//
//import com.comfenalco.servicio.exception.response.status.InternalServerErrorException;
//import org.apache.commons.net.ftp.FTPClient;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//
//public class FtpUploadFile {
//    private  String usuario;
//    private  String password;
//    private  String path;
//    private String ip = null;
//    private  int port;
//    private String pathLocal;
//    private String pathRemote;
//    private String nameRemote;
//
//    public FtpUploadFile(String usuario, String password, String ip, int port,String pathLocal,String pathRemote,String nameRemote) {
//        this.usuario = usuario;
//        this.password = password;
//        this.ip = ip;
//        this.port = port;
//        this.pathLocal = pathLocal;
//        this.pathRemote = pathRemote;
//        this.nameRemote = nameRemote;
//
//    }
//
//    public void ftpSendFile(){
//        String remoteFileName = this.pathRemote+this.nameRemote;
//        String localFileName = "file.txt";
//        File localFile = new File(localFileName);
//        FTPClient ftpClient = new FTPClient();
//        try{
//            ftpClient.connect(ip, port);
//            ftpClient.login(usuario, password);
//
//            InputStream inputStream = new FileInputStream(localFile);
//            ftpClient.setFileTransferMode(ftpClient.BINARY_FILE_TYPE);
//            ftpClient.enterLocalPassiveMode();
//            boolean done = ftpClient.storeFile(remoteFileName, inputStream);
//            int error = ftpClient.getReplyCode();
//            inputStream.close();
//            if (!done) {
//                System.out.println(error);
//                throw new InternalServerErrorException(String.valueOf(error));
//            }
//        }catch(IOException ex){
//            System.out.println("Error connecting to"+ ex.getMessage());
//            ex.printStackTrace();
//        }finally{
//            try{
//                if(ftpClient.isConnected()) {
//                    ftpClient.logout();
//                    ftpClient.disconnect();
//                }
//            }catch(IOException ex){
//                System.out.println(ex.getMessage());
//                ex.printStackTrace();
//            }
//        }
//    }
//}
