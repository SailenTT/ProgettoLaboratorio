package server;

import centrivaccinali.SingoloCentroVaccinale;
import cittadini.EventiAvversi;

import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.sql.Date;
import java.text.ParseException;
import java.util.Vector;


/**
 * Thread che funziona da slave del server. Ogni volta che il server apre una nuova connessione con il client, viene creata un'istanza di questa classe che gestirà poi lo scambio di informazioni tra client-server
 */
public class ServerHandler extends Thread{
    /**
     * Il codice dell'operazione che effettua il login di un utente
     */
    public static final int LOGIN_USER_OP_CODE =1;
    /**
    * Il codice dell'operazione che effettua la registrazione di un utente
     */
    public static final int REGISTER_USER_OP_CODE =2;
    /**
     * Il codice dell'operazione che effettua la registrazione della vaccinazione di un nuovo cittadino
     */
    public static final int REGISTER_VACCINATED_OP_CODE=3;
    /**
     * Il codice dell'operazione che effettua la registrazione di un nuovo centro vaccinale
     */
    public static final int REGISTER_CENTER_OP_CODE=4;
    /**
     * Il codice dell'operazione che invia al client la lista dei centri vaccinali nel database
     */
    public static final int GET_VAX_CENTERS_OP_CODE=5;
    /**
     * Il codice dell'operazione che invia al client la lista degli eventi avversi registrati presso un determinato centro vaccinali
     */
    public static final int GET_EVENTIAVVERSI_OP_CODE=6;
    /**
     * Il codice dell'operazione che riceve gli eventi avversi dell'utente
     */
    public static final int REGISTER_EVENTIAVVERSI_OP_CODE =7;
    /**
     * Il socket che permette di comunicare con il client
     */

    private Socket s;
    /**
     * Il buffer di dati primitivi in input dal client
     */
    private BufferedReader in;
    /**
     * Il buffer di dati primitivi in output al client
     */
    private PrintWriter out;
    /**
     * L'operation code ricevuto dal client sotto forma di stringa
     */
    private String op;
    /**
     * Gli ulteriori parametri della richiesta passati assieme all'operation code
     */
    private String parameters;
    /**
     * Il buffer di dati composti (classi) in ouput al client
     */
    private ObjectOutputStream os;
    /**
     * L'operation code ricevuto dal client convertito in un valore intero
     */
    private int op_converted;

    /**
     * Costruttore principale della classe.
     * @param s Il socket ricevuto dal server e che permette lo scambio di dati con il client
     */
    ServerHandler(Socket s){
      this.s = s;
      start();
    }

    private void login(String parameters) {//ricevo user e psw, connetto db e checko, il client sarà in ascolto e ritorno true o false
        String[] parameters_splitted = parameters.split(";");
        String email = parameters_splitted[0];
        String mail_db = "";
        String pwd_db = "";
        String pwd = parameters_splitted[1];

        try {
            Connection con = connectDB();
            PreparedStatement stm = con.prepareStatement("SELECT email,password FROM public.utente where email=? and password =?");
            stm.setString(1, email);
            stm.setString(2, pwd);
            ResultSet result = stm.executeQuery();
            while (result.next()) {
                mail_db = result.getString("email");

                pwd_db = result.getString("password");
            }
            if (email.equals(mail_db) && pwd.equals(pwd_db)) {
                System.out.println("[DB - THREAD] MATCH NEL DB");
                out.println("true");
            } else {
                System.out.println("[DB - THREAD]NO MATCH NEL DB");
                out.println("false");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void registerUser(String parameters) throws ParseException {
        String[] parameters_splitted = parameters.split(";");
        String name = parameters_splitted[0];
        String surname = parameters_splitted[1];
        String user = parameters_splitted[2];
        String userCF = parameters_splitted[3];
        String pwd = parameters_splitted[4];
        String dateBirth = parameters_splitted[5];
        Date date1 = java.sql.Date.valueOf(dateBirth);
        try{
            Connection con = connectDB();
            String sql ="insert into public.utente(nome,cognome,cf,data_nascita,email,password) values (?,?,?,?,?,?)" ;
            PreparedStatement stm = con.prepareStatement(sql);
            stm.setString(1,name);
            stm.setString(2,surname);
            stm.setString(3,userCF);
            stm.setDate(4,date1);
            stm.setString(5,user);
            stm.setString(6,pwd);
            int result = stm.executeUpdate();
            if(result>0){
                System.out.println("[DB -THREAD] QUERY REGISTRAZIONE COMPLETATA");
                out.println("true");
            }else {
                System.out.println("[DB - THREAD] QUERY REGISTRAZIONE ERRORE");
                out.println("false");
            }
        }catch (SQLException  e){
            e.printStackTrace();
        }
    }

    private void registerVaccinatedUser(String parameters){
        String[] parameters_splitted = parameters.split(";");
        String nome = parameters_splitted[0];
        String cognome = parameters_splitted[1];
        String codice_fiscale = parameters_splitted[2];
        String tipoVaccino = parameters_splitted[3];
        String centroVaccinale = parameters_splitted[4];
        String dataVaccinazione = parameters_splitted[5];
        Date dataVaccinazioneSQL = java.sql.Date.valueOf(dataVaccinazione);
        String idVaccinazione = parameters_splitted[6];
        System.out.println(dataVaccinazione);
        System.out.println(dataVaccinazioneSQL);
        try{
            Connection con = connectDB();
            String sql ="insert into vaccinati (nome,cognome,codice_fiscale,vaccino,centrovaccinale,data_vaccinazione,id_vaccinazione) VALUES (?,?,?,?,?,?,?)";
            PreparedStatement stm = con.prepareStatement(sql);
            stm.setString(1,nome);
            stm.setString(2,cognome);
            stm.setString(3,codice_fiscale);
            stm.setString(4,tipoVaccino);
            stm.setString(5,centroVaccinale);
            stm.setDate(6,dataVaccinazioneSQL);
            stm.setString(7,idVaccinazione);
            int result = stm.executeUpdate();
            if(result>0){
                System.out.println("[DB -THREAD] QUERY REGISTRAZIONE COMPLETATA");
                out.println("true");
            }else {
                System.out.println("[DB - THREAD] QUERY REGISTRAZIONE ERRORE");
                out.println("false");
            }
        }catch(SQLException e){
            e.printStackTrace();
        }

    }

    private void registerVaccineCenter(String parameters) {
        String[] parameters_splitted = parameters.split(";");
        String nome = parameters_splitted[0];
        String qualificatore = parameters_splitted[1];
        String via = parameters_splitted[2];
        String civico = parameters_splitted[3];
        String comune = parameters_splitted[4];
        String provincia = parameters_splitted[5];
        String cap = parameters_splitted[6];
        String tipologia = parameters_splitted[7];
        try {
            Connection con = connectDB();
            String sql = "INSERT INTO public.centrivaccinali (qualificatore,via,civico,comune,provincia,cap,tipologia,nome)\n" +
                    "VALUES(?,?,?,?,?,?,?,?)";
            PreparedStatement stm = con.prepareStatement(sql);
            stm.setString(1, qualificatore);
            stm.setString(2, via);
            stm.setString(3, civico);
            stm.setString(4, comune);
            stm.setString(5, provincia);
            stm.setString(6, cap);
            stm.setString(7, tipologia);
            stm.setString(8, nome);
            int result = stm.executeUpdate();
            if (result > 0) {
                System.out.println("[DB -THREAD] REGISTRATO CENTRO VACCINALE ");
                out.println("true");
            } else {
                System.out.println("[DB - THREAD] ERRORE REGISTRAZIONE CENTRO VACCINALE");
                out.println("false");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private void getCentriVaccinaliFromDb(){
        System.out.println("CHIAMATOOOOOOOO");
        Vector<SingoloCentroVaccinale> vector = new Vector<>();
        String nome_db;
        String qualificatore_db;
        String via_db;
        String civico_db;
        String comune_db;
        String provincia_db;
        String cap_db;
        String tipologia_db;
        try {
            Connection con = connectDB();
            String sql = "SELECT * from centrivaccinali";
            PreparedStatement stm = con.prepareStatement(sql);
            ResultSet res = stm.executeQuery();
            while(res.next()){
                nome_db = res.getString("nome");
                via_db = res.getString("via");
                qualificatore_db = res.getString("qualificatore");
                civico_db = res.getString("civico");
                comune_db = res.getString("comune");
                provincia_db = res.getString("provincia");
                cap_db = res.getString("cap");
                String indirizzo = via_db+" "+nome_db+", "+civico_db+", "+comune_db+" ("+provincia_db+") "+cap_db;
                tipologia_db = res.getString("tipologia");
                vector.add(new SingoloCentroVaccinale(nome_db, indirizzo, tipologia_db));
            }
            os = new ObjectOutputStream(s.getOutputStream());
            os.writeObject(vector);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getEventiAvversi(String idCentro){
        //faccio diviso 8 perché sono i campi per ogni paziente
        Vector<EventiAvversi> vector = new Vector<EventiAvversi>();
        int maleTesta;
        int febbre;
        int doloriMuscolari;
        int linfoadenopatia;
        int tachicardia;
        int crisiIpertensiva;
        String otherSimptoms;


        try {
            Connection con=connectDB();

            String sql="SELECT * FROM eventiavversi ea JOIN centrivaccinali cv ON ea.id=cv.id WHERE cv.id="+idCentro;
            PreparedStatement prepSt=con.prepareStatement(sql);
            ResultSet result=prepSt.executeQuery();
            while (result.next()){
                maleTesta=result.getInt("male_testa");
                febbre=result.getInt("febbre");
                doloriMuscolari=result.getInt("dolori_muscolari");
                linfoadenopatia=result.getInt("linfoadenopatia");
                tachicardia=result.getInt("tachicardia");
                crisiIpertensiva=result.getInt("crisi_ipertensiva");
                otherSimptoms=result.getString("altri_sintomi");
                EventiAvversi ev=new EventiAvversi(maleTesta,febbre,doloriMuscolari,linfoadenopatia,tachicardia,crisiIpertensiva,otherSimptoms);
                vector.add(ev);
            }

            ObjectOutputStream oos=new ObjectOutputStream(s.getOutputStream());
            oos.writeObject(vector);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private Connection connectDB() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/CentriVaccinali", "postgres", "admin");
        if (conn != null) {
            System.out.println("[DB - THREAD] - Sono connesso al db");//qui fa login
        } else {
            System.err.println("[DB - THREAD] - Non sono connesso al db");
        }
        return conn;
    }

    @Override
    public void run() {
        super.run();
        System.out.println("[THREAD] - Server thread startato");
        try{
            in = new BufferedReader(new InputStreamReader(s.getInputStream()));

            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true);
            while (true){
                System.out.println("[THREAD] Ascolto");
                parameters = in.readLine(); //qui impacchetto qualsiasi dato con separatore ";" per il server
                op = in.readLine(); //questo è l'operation code
                op_converted = Integer.parseInt(op);
                switch (op_converted) {
                    case LOGIN_USER_OP_CODE -> {
                        System.out.println("[THREAD] Login chiamata");
                        login(parameters);
                    }
                    case REGISTER_USER_OP_CODE -> {
                        System.out.println("[THREAD] Register chiamata");
                        registerUser(parameters);
                    }
                    case REGISTER_VACCINATED_OP_CODE -> {
                        System.out.println("[THREAD] Register vaccinati chiamata  ");
                        registerVaccinatedUser(parameters);
                    }
                    case REGISTER_CENTER_OP_CODE ->{
                        System.out.println("[THREAD] Nuovo Centro Vaccinale inserimento chiamata");
                        registerVaccineCenter(parameters);
                    }
                    case GET_VAX_CENTERS_OP_CODE ->{
                        System.out.println("[THREAD] Getter centri vaccinali chiamata");
                        getCentriVaccinaliFromDb();
                    }
                    case GET_EVENTIAVVERSI_OP_CODE ->{
                        System.out.println("[THREAD] Getter eventi avversi chiamata");
                        getEventiAvversi(parameters);
                    }
                    case REGISTER_EVENTIAVVERSI_OP_CODE ->{
                        System.out.println("[THREAD] Register eventi avversi chiamata");
                        getEventiAvversi(parameters);
                    }
                }
            }
        }catch (IOException e){} catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
