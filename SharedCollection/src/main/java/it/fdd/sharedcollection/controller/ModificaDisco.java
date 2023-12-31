package it.fdd.sharedcollection.controller;

import it.fdd.framework.result.FailureResult;

import javax.persistence.criteria.CriteriaBuilder;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import it.fdd.framework.data.DataException;
import it.fdd.framework.result.SplitSlashesFmkExt;
import it.fdd.framework.result.TemplateManagerException;
import it.fdd.framework.result.TemplateResult;
import it.fdd.framework.security.SecurityLayer;
import it.fdd.sharedcollection.data.dao.SharedCollectionDataLayer;
import it.fdd.sharedcollection.data.impl.*;
import it.fdd.sharedcollection.data.model.*;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Time;
import java.util.ArrayList;
import java.sql.Date;
import java.util.Iterator;
import java.util.List;

public class ModificaDisco extends SharedCollectionBaseController {

    private int collezione_key = 0;
    private int disco_key = 0;
    private String formato;

    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException {

        try {
            if (request.getParameter("numero") != null) {
                disco_key = SecurityLayer.checkNumeric(request.getParameter("numero"));
                collezione_key = SecurityLayer.checkNumeric(request.getParameter("collezione"));
                formato = request.getParameter("formato");
                request.setAttribute("discoID", disco_key);
                request.setAttribute("collezioneID", collezione_key);
                request.setAttribute("formato", formato);
                action_default(request, response);
            } else {
                if (request.getParameter("updateDisco") != null) {
                    action_modifica(request, response);
                } else if (request.getParameter("updateBrano") != null) {
                    action_updateBrano(request, response);
                } else if (request.getParameter("deleteBrano") != null) {
                    action_deleteBrano(request, response);
                } else if (request.getParameter("newBrano") != null) {
                    action_newBrano(request, response);
                } else {
                    action_immagini(request, response);
                }
                response.sendRedirect("collezioni");
            }
        } catch (NumberFormatException ex) {
            request.setAttribute("message", "Invalid number submitted");
            action_error(request, response);
        } catch (IOException | DataException ex) {
            request.setAttribute("exception", ex);
            action_error(request, response);
        }
    }

    private void action_error(HttpServletRequest request, HttpServletResponse response) {
        if (request.getAttribute("message") != null) {
            (new FailureResult(getServletContext())).activate((String) request.getAttribute("message"), request, response);
        }
    }

    private void action_default(HttpServletRequest request, HttpServletResponse response) throws IOException {
        TemplateResult res = new TemplateResult(getServletContext());
        HttpSession sessione = request.getSession(true);

        request.setAttribute("strip_slashes", new SplitSlashesFmkExt());
        request.setAttribute("collezioniPath", true);

        if (SecurityLayer.checkSession(request) != null) {
            request.setAttribute("session", true);
            request.setAttribute("username", sessione.getAttribute("username"));
            request.setAttribute("email", sessione.getAttribute("email"));
            request.setAttribute("userid", sessione.getAttribute("userid"));
        } else {
            response.sendRedirect("collezioni");
        }

        try {
            Collezione collezione = ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getCollezioneDAO().getCollezione(collezione_key);
            Disco disco = ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getDiscoDAO().getDisco(disco_key);
            List<ListaBrani> listaBrani = ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getListaBraniDAO().getListeBrani(disco_key);
            ListaDischi infoDisco = ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getListaDischiDAO().getListaDisco(collezione_key, disco_key, formato);
            List<Artista> lista_artisti = ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getArtistaDAO().getArtisti();
            List<Genere> lista_generi = ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getGenereDAO().getListaGeneri();
            List<Canzone> canzoni = new ArrayList<>();
            List<ListaArtisti> artisti = new ArrayList<>();
            List<ListaGeneri> generi = new ArrayList<>();

            request.setAttribute("page_title", disco.getNome());


            for (ListaBrani brano : listaBrani) {
                canzoni.add(((SharedCollectionDataLayer) request.getAttribute("datalayer")).getCanzoneDAO().getCanzone(brano.getCanzone().getKey()));
                artisti.addAll(((SharedCollectionDataLayer) request.getAttribute("datalayer")).getListaArtistiDAO().getListaArtistiByCanzone(brano.getCanzone().getKey()));
                generi.addAll(((SharedCollectionDataLayer) request.getAttribute("datalayer")).getListaGeneriDAO().getListaGeneriByCanzone(brano.getCanzone().getKey()));
            }

            request.setAttribute("collezione", collezione);
            request.setAttribute("disco", disco);
            request.setAttribute("listaBrani", listaBrani);
            request.setAttribute("infoDisco", infoDisco);
            request.setAttribute("lista_artisti", lista_artisti);
            request.setAttribute("lista_generi", lista_generi);
            request.setAttribute("canzoni", canzoni);
            request.setAttribute("listaArtisti", artisti);
            request.setAttribute("listaGeneri", generi);

            res.activate("modifica_disco.html.ftl", request, response);
        } catch (DataException ex) {
            request.setAttribute("exception", ex);
            action_error(request, response);
        } catch (TemplateManagerException e) {
            throw new RuntimeException(e);
        }

    }

    private void action_modifica(HttpServletRequest request, HttpServletResponse response) throws IOException, DataException {

        String titolo = request.getParameter("titolo");
        String etichetta = request.getParameter("etichetta");
        Date anno = Date.valueOf(request.getParameter("anno"));
        String error_msg;

        int numeroCopie = Integer.parseInt(request.getParameter("numeroCopie"));
        String stato = request.getParameter("stato");
        String barcode = request.getParameter("barcode");

        HttpSession sessione = request.getSession(true);
        int userID = 0;

        if (SecurityLayer.checkSession(request) != null) {
            userID = Integer.parseInt(sessione.getAttribute("userid").toString());
        }

        Disco disco = ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getDiscoDAO().getDisco(disco_key);

        if (userID == disco.getCreatore().getKey()) {

            if (titolo.isEmpty()) {
                error_msg = "Inserisci un nome per il disco!";
                request.setAttribute("error_", error_msg);
                action_default(request, response);
                return;
            }

            if (etichetta.isEmpty()) {
                error_msg = "Inserisci un'etichetta per il disco!";
                request.setAttribute("error_", error_msg);
                action_default(request, response);
                return;
            }

            // update disco
            disco.setNome(titolo);
            disco.setEtichetta(etichetta);
            disco.setAnno(anno);
            ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getDiscoDAO().storeDisco(disco);
        }

        ListaDischi listaDischi = ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getListaDischiDAO().getListaDisco(collezione_key, disco_key, formato);
        //update listaDIschi
        listaDischi.setNumeroCopie(numeroCopie);
        listaDischi.setStato(stato);
        listaDischi.setBarcode(barcode);
        ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getListaDischiDAO().storeListaDischi(listaDischi);

        response.sendRedirect("modificaDisco?numero=" + disco_key + "&collezione=" + collezione_key + "&formato=" + formato);

    }

    private void action_immagini(HttpServletRequest request, HttpServletResponse response) throws IOException, DataException {

        String imgCopertina = "images/templateimg/core-img/disco_default.jpeg";
        String imgFronte = null;
        String imgRetro = null;
        String imgLibretto = null;

        final long serialVersionUID = 1L;

        final int THRESHOLD_SIZE = 3096 * 3096 * 3;
        final int MAX_FILE_SIZE = 3096 * 3096 * 15;
        final int MAX_REQUEST_SIZE = 3096 * 3096 * 20;


        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(THRESHOLD_SIZE);
        factory.setRepository(new File(System.getProperty("java.io.tmpdir")));
        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setFileSizeMax(MAX_FILE_SIZE);
        upload.setSizeMax(MAX_REQUEST_SIZE);
        String uploadPath = System.getenv("PROJECT_IMG") + "/upload-img";

        File uploaddir = new File(uploadPath);
        if (!uploaddir.exists()) {
            uploaddir.mkdirs();
        }

        try {
            List formItems = upload.parseRequest(request);
            Iterator it = formItems.iterator();
            // iterates over form's fields
            while (it.hasNext()) {
                FileItem item = (FileItem) it.next();
                // processes only fields that are not form fields
                if (!item.isFormField()) {
                    //creazione cartella per le immagini della collezione
                    new File(uploadPath + File.separator + collezione_key).mkdir();
                    new File(request.getServletContext().getRealPath("/images/upload-img") + File.separator + collezione_key).mkdir();

                    String fileName = new File(item.getName()).getName();
                    System.out.println("fileName: " + fileName);
                    String filePath = uploadPath + File.separator + collezione_key + File.separator + fileName;
                    String filePath_ = request.getServletContext().getRealPath("/images/upload-img") + File.separator + collezione_key + File.separator + fileName;
                    System.out.println("filePath: " + filePath);
                    System.out.println("filePath_: " + filePath_);

                    File storeFile = new File(filePath);
                    File storeFile_ = new File(filePath_);
                    // saves the file on disk
                    item.write(storeFile);
                    item.write(storeFile_);

                    if ("imgCopertina".equals(item.getFieldName())) {
                        imgCopertina = "images/upload-img" + File.separator + collezione_key + File.separator + fileName;
                    }

                    if ("imgFronte".equals(item.getFieldName())) {
                        imgFronte = "images/upload-img" + File.separator + collezione_key + File.separator + fileName;
                    }

                    if ("imgRetro".equals(item.getFieldName())) {
                        imgRetro = "images/upload-img" + File.separator + collezione_key + File.separator + fileName;
                    }

                    if ("imgLibretto".equals(item.getFieldName())) {
                        imgLibretto = "images/upload-img" + File.separator + collezione_key + File.separator + fileName;
                    }
                }
            }
            PrintWriter out = response.getWriter();
        } catch (Exception e) {
            e.printStackTrace();
        }

        ListaDischi listaDischi = ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getListaDischiDAO().getListaDisco(collezione_key, disco_key, formato);
        if (!imgCopertina.equals("images/templateimg/core-img/disco_default.jpeg")) {
            listaDischi.setImgCopertina(imgCopertina);
        }
        if (imgFronte != null) {
            listaDischi.setImgFronte(imgFronte);
        }
        if (imgRetro != null) {
            listaDischi.setImgRetro(imgRetro);
        }
        if (imgLibretto != null) {
            listaDischi.setImgLibretto(imgLibretto);
        }
        ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getListaDischiDAO().storeListaDischi(listaDischi);

        response.sendRedirect("modificaDisco?numero=" + disco_key + "&collezione=" + collezione_key + "&formato=" + formato);
    }

    private void action_updateBrano(HttpServletRequest request, HttpServletResponse response) throws IOException, DataException {

        String nome = request.getParameter("nome");
        String durata_ = request.getParameter("durata");
        String[] generi = request.getParameterValues("selectGeneri");
        String[] artisti = request.getParameterValues("selectArtisti");
        int canzoneID = Integer.parseInt(request.getParameter("canzoneID"));

        Time durata = Time.valueOf("00:" + durata_);
        List<Integer> generiID = new ArrayList<>();
        List<Integer> artistiID = new ArrayList<>();


        for (String genere : generi) {
            generiID.add(Integer.parseInt(genere));
        }

        for (String artista : artisti) {
            artistiID.add(Integer.parseInt(artista));
        }

        // update canzone
        Canzone canzone = ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getCanzoneDAO().getCanzone(canzoneID);
        canzone.setNome(nome);
        canzone.setDurata(durata);
        ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getCanzoneDAO().storeCanzone(canzone);


        // eliminazione listaGeneri
        List<ListaGeneri> listaGeneri = ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getListaGeneriDAO().getListaGeneriByCanzone(canzoneID);
        for (ListaGeneri lista_generi : listaGeneri) {
            ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getListaGeneriDAO().deleteListaGeneri(lista_generi);
        }

        // eliminazione listaBrani
        List<ListaArtisti> listaArtisti = ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getListaArtistiDAO().getListaArtistiByCanzone(canzoneID);
        for (ListaArtisti lista_artisti : listaArtisti) {
            ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getListaArtistiDAO().deleteListaArtisti(lista_artisti);
        }

        // aggiunta listaGeneri
        for (int genereID : generiID) {
            ListaGeneri listaGenere = new ListaGeneriImpl();
            listaGenere.setGenere(((SharedCollectionDataLayer) request.getAttribute("datalayer")).getGenereDAO().getGenere(genereID));
            listaGenere.setCanzone(canzone);
            ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getListaGeneriDAO().storeListaGeneri(listaGenere);
        }

        // aggiunta listaArtisti
        for (int artistaID : artistiID) {
            ListaArtisti listaArtista = new ListaArtistiImpl();
            listaArtista.setArtista(((SharedCollectionDataLayer) request.getAttribute("datalayer")).getArtistaDAO().getArtista(artistaID));
            listaArtista.setCanzone(canzone);
            listaArtista.setRuolo("Entrambi");
            ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getListaArtistiDAO().storeListaArtisti(listaArtista);
        }

        response.sendRedirect("modificaDisco?numero=" + disco_key + "&collezione=" + collezione_key + "&formato=" + formato);
    }

    private void action_deleteBrano(HttpServletRequest request, HttpServletResponse response) throws IOException, DataException {
        ListaBrani brano = ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getListaBraniDAO().getListaBrani(Integer.parseInt(request.getParameter("branoID")));
        ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getListaBraniDAO().deleteListaBrani(brano);
        response.sendRedirect("modificaDisco?numero=" + disco_key + "&collezione=" + collezione_key + "&formato=" + formato);
    }

    private void action_newBrano(HttpServletRequest request, HttpServletResponse response) throws IOException, DataException {

        String nome = request.getParameter("nome");
        String durata_ = request.getParameter("durata");
        String[] generi = request.getParameterValues("selectGeneri");

        int[] artista = new int[4];
        artista[0] = Integer.parseInt(request.getParameter("selectArtisti1"));
        artista[1] = Integer.parseInt(request.getParameter("selectArtisti2"));
        artista[2] = Integer.parseInt(request.getParameter("selectArtisti3"));
        artista[3] = Integer.parseInt(request.getParameter("selectArtisti4"));
        String[] ruolo = new String[4];
        ruolo[0] = request.getParameter("ruolo1");
        ruolo[1] = request.getParameter("ruolo2");
        ruolo[2] = request.getParameter("ruolo3");
        ruolo[3] = request.getParameter("ruolo4");

        if (durata_.isEmpty()) {
            request.setAttribute("message", "Dati inseriti non validi!");
            action_error(request, response);
        }
        Time durata = Time.valueOf("00:" + durata_);

        if (generi == null) {
            request.setAttribute("message", "Dati inseriti non validi!");
            action_error(request, response);
        }
        List<Integer> generiID = new ArrayList<>();
        for (String genere : generi) {
            generiID.add(Integer.parseInt(genere));
        }

        if (nome.isEmpty()) {
            request.setAttribute("message", "Dati inseriti non validi!");
            action_error(request, response);
        }
        // aggiunta nuovo brano
        Canzone canzone = new CanzoneImpl();
        canzone.setNome(nome);
        canzone.setDurata(durata);
        ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getCanzoneDAO().storeCanzone(canzone);

        // istanza completa del brano appena creato
        Canzone nuova_canzone = ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getCanzoneDAO().getLast();

        if (generiID.isEmpty() || nuova_canzone == null) {
            request.setAttribute("message", "Dati inseriti non validi!");
            action_error(request, response);
        }
        // aggiunta dei generi in listaGeneri
        for (Integer id : generiID) {
            ListaGeneri listaGeneri = new ListaGeneriImpl();
            listaGeneri.setCanzone(nuova_canzone);
            listaGeneri.setGenere(((SharedCollectionDataLayer) request.getAttribute("datalayer")).getGenereDAO().getGenere(id));
            ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getListaGeneriDAO().storeListaGeneri(listaGeneri);
        }

        if (nuova_canzone == null) {
            request.setAttribute("message", "Dati inseriti non validi!");
            action_error(request, response);
        }
        // aggiunta in listaBrani del disco
        ListaBrani listaBrani = new ListaBraniImpl();
        listaBrani.setCanzone(nuova_canzone);
        listaBrani.setDisco(((SharedCollectionDataLayer) request.getAttribute("datalayer")).getDiscoDAO().getDisco(disco_key));
        ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getListaBraniDAO().storeListaBrani(listaBrani);

        for (int i = 0; i < 4; i++) {
            if (artista[i] == 0 || ruolo[i].isEmpty()) {
                if (i == 0) {
                    request.setAttribute("message", "Dati inseriti non validi!");
                    action_error(request, response);
                }
            } else {
                List<ListaArtisti> lista = ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getListaArtistiDAO().getListaArtistiByCanzone(nuova_canzone.getKey());
                for (ListaArtisti a : lista) {
                    if (a.getArtista().getKey() == artista[i]) {
                        request.setAttribute("message", "Artista già presente!");
                        action_error(request, response);
                    }
                }
                // aggiunta in listaArtisti1
                ListaArtisti listaArtisti = new ListaArtistiImpl();
                listaArtisti.setArtista(((SharedCollectionDataLayer) request.getAttribute("datalayer")).getArtistaDAO().getArtista(artista[i]));
                listaArtisti.setCanzone(nuova_canzone);
                listaArtisti.setRuolo(ruolo[i]);
                ((SharedCollectionDataLayer) request.getAttribute("datalayer")).getListaArtistiDAO().storeListaArtisti(listaArtisti);
            }
        }

        response.sendRedirect("modificaDisco?numero=" + disco_key + "&collezione=" + collezione_key + "&formato=" + formato);
    }

}
