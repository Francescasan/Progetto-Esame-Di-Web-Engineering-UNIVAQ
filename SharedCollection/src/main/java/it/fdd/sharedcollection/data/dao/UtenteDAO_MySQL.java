package it.fdd.sharedcollection.data.dao;

import it.fdd.framework.data.*;
import it.fdd.sharedcollection.data.model.Utente;
import it.fdd.sharedcollection.data.proxy.UtenteProxy;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class UtenteDAO_MySQL extends DAO implements UtenteDAO {
    private PreparedStatement sUtenti, sUtenteByID, sUtentiByNickname;
    private PreparedStatement iUtente;

    private PreparedStatement uUtente;
    private PreparedStatement login;
    private PreparedStatement getUtenteByUsername;
    private PreparedStatement loginUtente;
    private PreparedStatement sToken, sLink, insertLink, insertToken;

    private PreparedStatement getUtentiByLetter;

    private PreparedStatement sCollezioni, sNCollezioni, sDischi, sNDischi, sNBrani;

    public UtenteDAO_MySQL(DataLayer d) {
        super(d);
    }

    @Override
    public void init() throws DataException {
        try {
            super.init();

            //precompiliamo tutte le query utilizzate nella classe
            //precompile all the queries uses in this class
            sUtenteByID = connection.prepareStatement("SELECT * FROM Utente WHERE ID=?");
            sUtenti = connection.prepareStatement("SELECT id FROM Utente");
            login = connection.prepareStatement("SELECT * FROM Utente WHERE email = ? AND password = ?");
            iUtente = connection.prepareStatement("INSERT INTO utente (nickname,email,password,nome,cognome) VALUES (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            uUtente = connection.prepareStatement("UPDATE Utente SET nome = ?, cognome = ?, password = ? WHERE id = ?");
            getUtenteByUsername = connection.prepareStatement("SELECT id FROM Utente WHERE nickname = ?");
            sUtentiByNickname = connection.prepareStatement("SELECT * FROM Utente WHERE MATCH (nickname) AGAINST (? IN NATURAL LANGUAGE MODE)");

            getUtentiByLetter = connection.prepareStatement("SELECT *FROM Utente WHERE nickname LIKE ?");

            loginUtente = connection.prepareStatement("SELECT id, password FROM Utente WHERE email = ?");

            sToken = connection.prepareStatement("SELECT token FROM Utente WHERE id = ?");
            sLink = connection.prepareStatement("SELECT * FROM Utente WHERE link = ?");
            insertLink = connection.prepareStatement("UPDATE Utente SET  link = ? WHERE id = ?");
            insertToken = connection.prepareStatement("UPDATE Utente SET  token = ? WHERE id = ?");

            sCollezioni = connection.prepareStatement("SELECT *FROM Collezione WHERE utente = ?");
            sNCollezioni = connection.prepareStatement("SELECT COUNT(id) AS c FROM Collezione WHERE utente = ? ");
            sDischi = connection.prepareStatement("SELECT *FROM ListaDischi WHERE collezione = ?");
            sNDischi = connection.prepareStatement("SELECT COUNT(id) AS c FROM ListaDischi WHERE collezione = ?");
            sNBrani = connection.prepareStatement("SELECT COUNT(id) AS c FROM ListaBrani WHERE disco = ?");

        } catch (SQLException ex) {
            throw new DataException("Error initializing newspaper data layer", ex);
        }
    }

    @Override
    public void destroy() throws DataException {
        //anche chiudere i PreparedStamenent � una buona pratica...
        //also closing PreparedStamenents is a good practice...
        try {
            sUtenteByID.close();
            sUtenti.close();
            login.close();
            iUtente.close();
            uUtente.close();
            loginUtente.close();
            getUtenteByUsername.close();
            sUtentiByNickname.close();
            sCollezioni.close();
            sNCollezioni.close();
            sDischi.close();
            sNDischi.close();
            sNBrani.close();
            sToken.close();
            sLink.close();
            insertLink.close();
            insertToken.close();
            getUtentiByLetter.close();
        } catch (SQLException ex) {
            //
        }
        super.destroy();
    }

    //metodi "factory" che permettono di creare
    //e inizializzare opportune implementazioni
    //delle interfacce del modello dati, nascondendo
    //all'utente tutti i particolari
    //factory methods to create and initialize
    //suitable implementations of the data model interfaces,
    //hiding all the implementation details
    @Override
    public UtenteProxy createUtente() {
        return new UtenteProxy(getDataLayer());
    }

    //helper
    private UtenteProxy createUtente(ResultSet rs) throws DataException {
        try {
            UtenteProxy a = createUtente();
            a.setKey(rs.getInt("id"));
            a.setNome(rs.getString("nome"));
            a.setCognome(rs.getString("cognome"));
            a.setNickname(rs.getString("nickname"));
            a.setPassword(rs.getString("password"));
            a.setEmail(rs.getString("email"));
            a.setToken(rs.getInt("token"));
            a.setLink(rs.getString("link"));
            return a;
        } catch (SQLException ex) {
            throw new DataException("Unable to create Utente object form ResultSet", ex);
        }
    }

    @Override
    public Utente getUtente(int utente_key) throws DataException {
        Utente a = null;
        //prima vediamo se l'oggetto è già stato caricato
        //first look for this object in the cache
        if (dataLayer.getCache().has(Utente.class, utente_key)) {
            a = dataLayer.getCache().get(Utente.class, utente_key);
        } else {
            //altrimenti lo carichiamo dal database
            //otherwise load it from database
            try {
                sUtenteByID.setInt(1, utente_key);
                try (ResultSet rs = sUtenteByID.executeQuery()) {
                    if (rs.next()) {
                        //notare come utilizziamo il costrutture
                        //"helper" della classe UtenteImpl
                        //per creare rapidamente un'istanza a
                        //partire dal record corrente
                        //note how we use here the helper constructor
                        //of the UtenteImpl class to quickly
                        //create an instance from the current record

                        a = createUtente(rs);
                        //e lo mettiamo anche nella cache
                        //and put it also in the cache
                        dataLayer.getCache().add(Utente.class, a);
                    }
                }
            } catch (SQLException ex) {
                throw new DataException("Unable to load Utente by ID", ex);
            }
        }
        return a;
    }

    @Override
    public List<Utente> getUtenti() throws DataException {
        List<Utente> result = new ArrayList();

        try (ResultSet rs = sUtenti.executeQuery()) {
            while (rs.next()) {
                result.add(getUtente(rs.getInt("id")));
            }
        } catch (SQLException ex) {
            throw new DataException("Unable to load Utentes", ex);
        }
        return result;
    }

    @Override
    public HashSet<Utente> getUtenti(String nickname) throws DataException {

        HashSet<Utente> utenti = new HashSet<>();

        try {
            sUtentiByNickname.setString(1, nickname);
            try (ResultSet rs = sUtentiByNickname.executeQuery()) {
                while (rs.next()) {
                    utenti.add(getUtente(rs.getInt("id")));
                }
            }
        } catch (SQLException ex) {
            throw new DataException("Unable to load Utenti", ex);
        }
        return utenti;
    }

    @Override
    public Utente login(String email, String password) throws DataException {

        Utente utente = null;

        try {
            login.setString(1, email);
            login.setString(2, password);

            try (ResultSet rs = login.executeQuery()) {
                if (rs.next()) {
                    utente = getUtente(rs.getInt("id"));
                }
            }
        } catch (SQLException ex) {
            throw new DataException("User not found", ex);
        }
        return utente;
    }

    @Override
    public List<Integer> getUtenteInfo(int utente_key) throws DataException {
        List<Integer> numeri = new ArrayList<>();
        List<Integer> id_collezini = new ArrayList<>();
        List<Integer> id_dischi = new ArrayList<>();

        //conto le collezioni
        try {
            sNCollezioni.setInt(1, utente_key);
            try (ResultSet rs = sNCollezioni.executeQuery()) {
                while (rs.next()) {
                    numeri.add(rs.getInt("c"));//numero collezioni
                }
            }
        } catch (SQLException ex) {
            throw new DataException("User not found", ex);
        }

        //Prendo gli id delle colelzioni
        try {
            sCollezioni.setInt(1, utente_key);
            try (ResultSet rs = sCollezioni.executeQuery()) {
                while (rs.next()) {
                    id_collezini.add(rs.getInt("id"));//id collezioni
                }
            }
        } catch (SQLException ex) {
            throw new DataException("User not found", ex);
        }

        //conto i dischi
        int sum = 0;
        for (Integer id : id_collezini) {
            try {
                sNDischi.setInt(1, id);
                try (ResultSet rs = sNDischi.executeQuery()) {
                    while (rs.next()) {
                        sum += rs.getInt("c"); //numero dischi
                    }
                }
            } catch (SQLException ex) {
                throw new DataException("User not found", ex);
            }

            //prendo gli id dei dischi
            try {
                sDischi.setInt(1, id);
                try (ResultSet rs = sDischi.executeQuery()) {
                    while (rs.next()) {
                        id_dischi.add(rs.getInt("disco")); //id dischi
                    }
                }
            } catch (SQLException ex) {
                throw new DataException("User not found", ex);
            }
        }
        numeri.add(sum);

        sum = 0;

        for (Integer id : id_dischi) {
            try {
                sNBrani.setInt(1, id);
                try (ResultSet rs = sNBrani.executeQuery()) {
                    while (rs.next()) {
                        sum = sum + rs.getInt("c"); //numero brani
                    }
                }
            } catch (SQLException ex) {
                throw new DataException("User not found", ex);
            }
        }
        numeri.add(sum);


        return numeri;
    }


    @Override
    public int getToken(Utente utente) throws DataException {
        int ptoken = 0;

        try {
            sToken.setInt(1, utente.getKey());
            try (ResultSet rs = sToken.executeQuery()) {
                if (rs.next()) {
                    ptoken = rs.getInt("token");

                }
            }
        } catch (SQLException ex) {
            throw new DataException("Unable to load token utente by email", ex);
        }
        return ptoken;
    }


    @Override
    public Utente getLink(String link) throws DataException {

        if (dataLayer.getCache().has(Utente.class, link)) {
            return dataLayer.getCache().get(Utente.class, link);
        }

        Utente utente = null;

        try {
            sLink.setString(1, link);
            try (ResultSet rs = sLink.executeQuery()) {
                if (rs.next()) {
                    utente = getUtente(rs.getInt("id"));

                }
            }
        } catch (SQLException ex) {
            throw new DataException("Unable to load link utente by email", ex);
        }

        return utente;
    }

    @Override
    public Utente getUtente(String email) throws DataException {

        if (dataLayer.getCache().has(Utente.class, email)) {
            return dataLayer.getCache().get(Utente.class, email);
        }

        Utente utente = null;

        try {
            loginUtente.setString(1, email);
            try (ResultSet rs = loginUtente.executeQuery()) {
                if (rs.next()) {
                    utente = getUtente(rs.getInt("id"));
                }
            }
        } catch (SQLException ex) {
            throw new DataException("Unable to load utente by email", ex);
        }
        return utente;
    }


    public String getPassword(String email) throws DataException {

        if (dataLayer.getCache().has(Utente.class, email)) {
            Utente utente = dataLayer.getCache().get(Utente.class, email);
            return utente.getPassword();
        }

        String pass = null;

        try {
            loginUtente.setString(1, email);
            try (ResultSet rs = loginUtente.executeQuery()) {
                if (rs.next()) {
                    pass = rs.getString("password");
                }
            }
        } catch (SQLException ex) {
            throw new DataException("Unable to load password utente by email", ex);
        }
        return pass;
    }


    @Override
    public Utente getUtenteByUsername(String username) throws DataException {

        if (dataLayer.getCache().has(Utente.class, username)) {
            return dataLayer.getCache().get(Utente.class, username);
        }

        Utente utente = null;

        try {
            getUtenteByUsername.setString(1, username);
            try (ResultSet rs = getUtenteByUsername.executeQuery()) {
                if (rs.next()) {
                    utente = getUtente(rs.getInt("id"));
                }
            }
        } catch (SQLException ex) {
            throw new DataException("Unable to load utente by usernme", ex);
        }
        return utente;
    }

    public Utente storeUtente(Utente utente) throws DataException {
        try {
            if (utente.getKey() != null && utente.getKey() > 0) {

                // non facciamo nulla se l'oggetto è un proxy e indica di non aver subito modifiche
                if (utente instanceof DataItemProxy && !((DataItemProxy) utente).isModified()) {
                    return null;
                }

                // update


                uUtente.setString(1, utente.getNome());
                uUtente.setString(2, utente.getCognome());
                uUtente.setString(3, utente.getPassword());


                if (uUtente.executeUpdate() == 0) {
                    throw new OptimisticLockException(utente);
                }

            } else {
                // insert
                iUtente.setString(1, utente.getNickname());
                iUtente.setString(2, utente.getEmail());
                iUtente.setString(3, utente.getPassword());
                iUtente.setString(4, utente.getNome());
                iUtente.setString(5, utente.getCognome());


                if (iUtente.executeUpdate() == 1) {
                    // get della chiave generata
                    try (ResultSet keys = iUtente.getGeneratedKeys()) {
                        if (keys.next()) {
                            int key = keys.getInt(1);
                            // update chiave
                            utente.setKey(key);
                            // inserimento nella cache
                            dataLayer.getCache().add(Utente.class, utente);
                        }
                    }
                }
            }
            // reset attributo dirty
            if (utente instanceof DataItemProxy) {
                ((DataItemProxy) utente).setModified(false);
            }
        } catch (SQLException | OptimisticLockException ex) {
            throw new DataException("Impossibile salvare l'utente", ex);
        }
        return utente;
    }

    @Override
    public List<Utente> getUtentiByLettera(String letter) throws DataException {

        List<Utente> utenti = new ArrayList<>();

        String letterk = letter+"%";

        try {
            getUtentiByLetter.setString(1, letterk);
            try (ResultSet rs = getUtentiByLetter.executeQuery()) {
                while (rs.next()) {
                    utenti.add(getUtente(rs.getInt("id")));
                }
            }
        } catch (SQLException ex) {
            throw new DataException("Unable to load Utenti", ex);
        }
        return utenti;
    }


    public void insertLink(Utente utente, String link) throws DataException {
        try {
            insertLink.setInt(2, utente.getKey());
            insertLink.setString(1, link);

            if (insertLink.executeUpdate() == 0) {
                throw new OptimisticLockException(utente);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void insertToken(Utente utente, int token) throws DataException {
        try {
            insertToken.setInt(2, utente.getKey());
            insertToken.setInt(1, token);

            if (insertToken.executeUpdate() == 0) {
                throw new OptimisticLockException(utente);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}
