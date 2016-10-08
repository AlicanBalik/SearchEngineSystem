package controllers;

import static play.data.Form.form; // //

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.commons.codec.binary.Base64; // //
import org.apache.commons.mail.*;

import models.Contact;
import models.Contribution;
import models.Index;
import models.Search;
import models.SearchQuery;
import models.User;

import play.Play;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;
import views.html.*;

public class Application extends Controller {

	final static Form<User> userForm = form(User.class);

	final static Form<Search> searchForm = form(Search.class);
	final static List<Search> searchList = new ArrayList<Search>();

	final static Form<SearchQuery> searchQueryForm = form(SearchQuery.class);

	final static Form<Contribution> contributeForm = form(Contribution.class);
	final static List<Contribution> contributeList = new ArrayList<Contribution>();

	final static Form<Index> indexForm = form(Index.class);
	
	final static Form<Contact> contactForm = form(Contact.class);

	static HttpSolrClient solr = null;
	static Configuration config = HBaseConfiguration.create();




	public static Result index() {
		Form<SearchQuery> filledForm = searchQueryForm.bindFromRequest();
		SearchQuery q = filledForm.get();
		return ok(index.render(q, contactForm));
	}

	public static Result download(String id) throws IOException {
		Get g = new Get(Bytes.toBytes(id));
		g.addColumn(Bytes.toBytes("content"), Bytes.toBytes("raw"));
		g.addColumn(Bytes.toBytes("book"), Bytes.toBytes("fileName"));

		HTable hTable = new HTable(config, "books");
		org.apache.hadoop.hbase.client.Result result = hTable.get(g);

		if (result.containsColumn(Bytes.toBytes("content"), Bytes.toBytes("raw"))) {
			byte[] rawBook = result.getNoVersionMap().get(Bytes.toBytes("content")).get(Bytes.toBytes("raw"));
			byte[] fileName = result.getNoVersionMap().get(Bytes.toBytes("book")).get(Bytes.toBytes("fileName"));
			String fn = new String(fileName, "UTF-8");
			response().setContentType("application/octet-stream");
			response().setHeader("Content-disposition", "attachment; filename=" + fn + ".pdf");

			Increment inc = new Increment(Bytes.toBytes(id));
			inc.addColumn(Bytes.toBytes("book"), Bytes.toBytes("downloadCounter"), 1L);
			hTable.increment(inc);
			return ok(rawBook);
		}
		hTable.close();
		return play.mvc.Results
				.notFound(
						"<h1>Something went wrong! Please try again.</h1><br><button onclick='goBack()'>Go Back</button><script>function goBack() { window.history.back();}</script>")
				.as("text/html");
	}
	
	public static void showResult(Search searched, SolrQuery query) throws SolrServerException, IOException {
			QueryResponse response = solr.query(query);
			SolrDocumentList results = response.getResults();
			HTable hTable = new HTable(config, "books");
			for (int i = 0; i < results.size(); ++i) {
				searched = new Search();
				searched.setOutputId((String) results.get(i).getFirstValue("id"));
				searched.setOutputTitle((String) results.get(i).getFirstValue("title"));
				searched.setOutputAuthor((String) results.get(i).getFirstValue("author"));
				searched.setOutputPage(results.get(i).getFirstValue("pageNumber").toString());
				searched.setOutputCreationDate(results.get(i).getFirstValue("creationDate").toString());
				searched.setOutputProducer((String) results.get(i).getFirstValue(("producer")));
				searched.setOutputFound(results.getNumFound());
//				System.out.println(results.get(i).getFirstValue("creationDate") instanceof Date);
				Get g = new Get(Bytes.toBytes(searched.getOutputId()));
				g.addColumn(Bytes.toBytes("picture"), Bytes.toBytes("raw"));
				g.addColumn(Bytes.toBytes("book"), Bytes.toBytes("downloadCounter"));
				org.apache.hadoop.hbase.client.Result result = hTable.get(g);
				if (result.containsColumn(Bytes.toBytes("picture"), Bytes.toBytes("raw"))) {
					byte[] byteOutputPicture = result.getNoVersionMap().get(Bytes.toBytes("picture"))
							.get(Bytes.toBytes("raw"));
					BufferedImage image = ImageIO.read(new ByteArrayInputStream(byteOutputPicture));
					java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
					ImageIO.write(image, "png", baos);
					searched.setOutputPicture(Base64.encodeBase64String(baos.toByteArray()));
				}
				
				searched.setOutputDownloadCounter(Bytes.toLong(
						result.getNoVersionMap().get(Bytes.toBytes("book")).get(Bytes.toBytes("downloadCounter"))));
				searchList.add(searched);
			}
			System.out.println(searched.getOutputFound());
			System.out.println(results.getNumFound());
			solr.close();
			hTable.close();
	}
	
	public static void solrConfiguration() {
		solr = new HttpSolrClient("http://" + play.Play.application().configuration().getString("solr.engine.host")
			      + ":" + play.Play.application().configuration().getString("solr.engine.port") + 
			       play.Play.application().configuration().getString("solr.engine.indexPath") + 
			        play.Play.application().configuration().getString("solr.engine.collection"));
	}
	

	public static Result search(int start) throws SolrServerException, IOException, ParseException {
		// trazi
		
		Form<SearchQuery> queryForm = searchQueryForm.bindFromRequest();
		SearchQuery q = queryForm.get();
		searchList.clear();
		Form<Search> filledForm = searchForm.bindFromRequest();
		Search searched = filledForm.get();
		if (q.getQ().length() < 400 || !q.getQ().isEmpty()) {
			solrConfiguration();
			
			SolrQuery query = (new SolrQuery(q.getQ())
					.setFacet(true)
//					.addFilterQuery("creationDate:2010-12-30 TO creationDate:2011-12-30") //yyyy-MM-dd
					.setStart(start*10)
					.setRows(10)
//					.addSort("id",ORDER.desc)
					);
//			System.out.println(query.getParameterNames());
			query.setFields("id", "title", "author", "pageNumber", "content", "path", "creationDate", "producer");
			
//			showResult(searched,query);
			QueryResponse response = solr.query(query);
			SolrDocumentList results = response.getResults();
			HTable hTable = new HTable(config, "books");
			for (int i = 0; i < results.size(); ++i) {
				searched = new Search();
				searched.setOutputId((String) results.get(i).getFirstValue("id"));
				searched.setOutputTitle((String) results.get(i).getFirstValue("title"));
				searched.setOutputAuthor((String) results.get(i).getFirstValue("author"));
				searched.setOutputPage(results.get(i).getFirstValue("pageNumber").toString());
				searched.setOutputCreationDate(results.get(i).getFirstValue("creationDate").toString());
				searched.setOutputProducer((String) results.get(i).getFirstValue(("producer")));
				searched.setOutputFound(results.getNumFound());
//				System.out.println(results.get(i).getFirstValue("creationDate") instanceof Date);
				Get g = new Get(Bytes.toBytes(searched.getOutputId()));
				g.addColumn(Bytes.toBytes("picture"), Bytes.toBytes("raw"));
				g.addColumn(Bytes.toBytes("book"), Bytes.toBytes("downloadCounter"));
				org.apache.hadoop.hbase.client.Result result = hTable.get(g);
				if (result.containsColumn(Bytes.toBytes("picture"), Bytes.toBytes("raw"))) {
					byte[] byteOutputPicture = result.getNoVersionMap().get(Bytes.toBytes("picture"))
							.get(Bytes.toBytes("raw"));
					BufferedImage image = ImageIO.read(new ByteArrayInputStream(byteOutputPicture));
					java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
					ImageIO.write(image, "png", baos);
					searched.setOutputPicture(Base64.encodeBase64String(baos.toByteArray()));
				}
				
				searched.setOutputDownloadCounter(Bytes.toLong(
						result.getNoVersionMap().get(Bytes.toBytes("book")).get(Bytes.toBytes("downloadCounter"))));
				searchList.add(searched);
			}
			int startplus = start + 1;
			int max = (int) searched.getOutputFound()/10;
			hTable.close();
			solr.close();
			return play.mvc.Results.ok(search.render(searched, searchForm, searchList, q, contactForm, start, startplus, max));
		} else {
			return notFound("<h1>404 NOT FOUND</h1><br><a href='/'>Back</a>").as("text/html");
		}
	}

	public static Result login() {
		if (null == session("username") || "-1".equals(session("username"))) {
			return ok(login.render(userForm));
		} else {
			return redirect(routes.Application.adminPanel());
		}
	}
	
	
	public static Result submit() throws IOException {
		Form<User> filledForm = userForm.bindFromRequest();
		User created = filledForm.get();

		if (created.getUsername() != null) {

			HTable hTable = new HTable(config, "accounts");

			Get get = new Get(Bytes.toBytes(created.getUsername()));
			org.apache.hadoop.hbase.client.Result result = hTable.get(get);

			byte[] bfullName = result.getValue(Bytes.toBytes("info"), Bytes.toBytes("full_name"));
			byte[] bPassword = result.getValue(Bytes.toBytes("info"), Bytes.toBytes("password"));
			byte[] bemail = result.getValue(Bytes.toBytes("info"), Bytes.toBytes("email"));

			if (created.getPassword().equals(Bytes.toString(bPassword))) {
				session("username", created.getUsername());
				session("fullname", Bytes.toString(bfullName));

				Date d = new Date();
				SimpleDateFormat f = new SimpleDateFormat("dd/MM/YY hh:mm:ss");
				System.out.println("Login Time: " + f.format(d));
				System.out.println("---------- Successfully logged it ----------");
				System.out.printf("---------- User %s email %s ----------  \n", Bytes.toString(bfullName),
						Bytes.toString(bemail));
				return redirect(routes.Application.adminPanel());
			} else {
				filledForm.data().put("message", "Invalid Username or Password");

				return ok(login.render(filledForm));
			}
		}
		return play.mvc.Results
				.notFound(
						"<h1>Something went wrong! Please try again.</h1><br><button onclick='goBack()'>Go Back</button><script>function goBack() { window.history.back();}</script>")
				.as("text/html");
	}

	public static Result contact() {
		return ok(contact.render(contactForm));
	}
	
	public static void setEmail(Form<Contact> filledForm) {
		try {
			Contact created = filledForm.get();
			
			String hostName = Play.application().configuration().getString("email.host");
			String userName = Play.application().configuration().getString("email.user");
			String password = Play.application().configuration().getString("email.password");
			HtmlEmail email = new HtmlEmail();
			email.setHostName(hostName);
			email.setSmtpPort(465);
			email.setAuthenticator(new DefaultAuthenticator(userName, password));
			email.setSSLOnConnect(true);
			email.setStartTLSEnabled(true);
			email.setFrom(created.getEmail());
			email.setSubject(created.getTitle());
			email.setHtmlMsg("<b>Title:</b> " + created.getTitle() +
					"<br><b>Email:</b> " + created.getEmail() + 
					"<br><b>Name Surname:</b> " + created.getNameSurname() + 
					"<br><b>Message:</b> " + created.getMessage());
			email.addTo("can1903ali@gmail.com");
			email.send();
		} catch (Exception e) {
			System.out.println("Contact error: " + e + " " + e.getMessage());
		}
	}
	
	public static Result submitContact() {
		Form<Contact> filledForm = contactForm.bindFromRequest();
		if (filledForm.hasErrors())
			filledForm.data().put("alert", "Wrong input for email");
			
		setEmail(filledForm);
		
		return redirect("/");
	}

	public static Result upload() {
		if (null == session("username") || "-1".equals(session("username"))) {
			return ok(login.render(userForm));
		} else {
			return ok(upload.render());
		}
	}

	public static String getProperDate(Calendar cal) {
		String properDate = null;
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		if (cal == null) {
			Date now = new Date();
			properDate = dateFormat.format(now);
			return properDate;
		}
		properDate = dateFormat.format(cal.getTime());
		return properDate;
	}
	
	public static void indexSolr() {
		
	}

	public static Result uploadPost() throws IOException, SolrServerException, IllegalArgumentException {
		config.get("hbase.client.keyvalue.maxsize");
		config.set("hbase.client.keyvalue.maxsize", "-1");

		Form<Index> filledForm = indexForm.bindFromRequest();
		Index indexed = filledForm.get();
		List<FilePart> fileParts = request().body().asMultipartFormData().getFiles();
		byte[] byteImage = null;
		HTable hTable = new HTable(config, "books");
		for (FilePart pdf : fileParts) {
			String fileName = pdf.getFilename();
			final int lastPeriodPos = fileName.lastIndexOf('.');
			if (!(lastPeriodPos <= 0))
				fileName = fileName.substring(0, lastPeriodPos);

			File file = pdf.getFile();
			// File filePathinServer = new File("pdfs", fileName);
			// parsing
			PDDocument document = null; // it only allows application/pdf content type, so there is no need to check contentType of file. PDFBOX already checks it.
			try{
			document = PDDocument.load(file, "");
			} catch (Exception e){
				return notFound(
						"<h1>Please choose PDF formatted file.</h1><br><button onclick='goBack()'>Go Back</button><script>function goBack() { window.history.back();}</script>")
								.as("text/html");
			}
			PDFTextStripper pdfstripper = new PDFTextStripper();
			PDDocumentInformation info = document.getDocumentInformation();
			indexed.setnPage(Integer.toString(document.getNumberOfPages()));
			indexed.setTitle(info.getTitle());
			indexed.setAuthor(info.getAuthor());
			indexed.setContent(pdfstripper.getText(document));
			indexed.setCreationDate(getProperDate(info.getCreationDate()));
			indexed.setProducer(info.getProducer());
			indexed.setFileName(fileName);

			//////////////////////////////////////////////////////////////
			//////// Books might have empty detail. Set a value for an empty detail temporarily. //////
			String gAuthor = indexed.getAuthor();
			String gProducer = indexed.getProducer();
			String gTitle = indexed.getTitle();
			if (gAuthor == null)
				indexed.setAuthor("NoAuthor");// *
			if (gProducer == null)
				indexed.setProducer("NoProducer");// *
			if (gTitle == null)
				indexed.setTitle("NoTitle"); // *
			//////////////////////////////////////////////////////////////

			

			solrConfiguration();
			
			SolrInputDocument doc = new SolrInputDocument();

			Set<String> indexedContentSet = new HashSet<>();
			Set<String> set = new HashSet<>();

			String arr[] = indexed.getContent().split("\\s+");
			for (String y : arr) {

				if (y.length() > 4) {

					if (set.contains(y)) { // ako set ima element y, onda to je
											// duplicate. dodaj element
											// y u indexedContentSet
						indexedContentSet.add(y.toLowerCase());
					}

					set.add(y.toLowerCase()); // dodaj sve elementa koje su
												// unique.
				}
			}
			ArrayList<String> allTogether = new ArrayList<>();
			allTogether.addAll(set);
			allTogether.addAll(indexedContentSet);

			indexed.setRandom(UUID.randomUUID().toString());

			Put p = new Put(Bytes.toBytes(indexed.getRandom()));
			try {
				PDFRenderer pdfRenderer = new PDFRenderer(document);
				if (document.isEncrypted()) {
					try {
						// document.decrypt("");
						document.setAllSecurityToBeRemoved(true);
						System.out.println("Uploaded file was encrypted. It has been successfully decrypted.");
					} catch (Exception e) {
						throw new Exception("cannot be decrypted. ", e);
					}
				}

				BufferedImage image = new BufferedImage(200, 150, BufferedImage.TYPE_3BYTE_BGR);
				image = pdfRenderer.renderImageWithDPI(0, 50);
				java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
				ImageIO.write(image, "png", bos);
				byteImage = bos.toByteArray();

				// ImageIOUtil.writeImage(image , fileName+".jpg" ,300);

				document.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

			FileInputStream fin = new FileInputStream(file);
			p.add(Bytes.toBytes("content"), Bytes.toBytes("raw"), IOUtils.toByteArray(fin));
			fin.close();
			p.add(Bytes.toBytes("picture"), Bytes.toBytes("raw"), byteImage);

			p.add(Bytes.toBytes("book"), Bytes.toBytes("fileName"), Bytes.toBytes(fileName));
			p.add(Bytes.toBytes("book"), Bytes.toBytes("author"), Bytes.toBytes(indexed.getAuthor()));
			p.add(Bytes.toBytes("book"), Bytes.toBytes("content"), Bytes.toBytes(allTogether.toString()));
			p.add(Bytes.toBytes("book"), Bytes.toBytes("nPage"), Bytes.toBytes(indexed.getnPage()));
			p.add(Bytes.toBytes("book"), Bytes.toBytes("title"), Bytes.toBytes(indexed.getTitle()));
			p.add(Bytes.toBytes("book"), Bytes.toBytes("creationDate"), Bytes.toBytes(indexed.getCreationDate().toString()));
			p.add(Bytes.toBytes("book"), Bytes.toBytes("producer"), Bytes.toBytes(indexed.getProducer()));
			p.add(Bytes.toBytes("book"), Bytes.toBytes("fileName"), Bytes.toBytes(indexed.getFileName()));
			p.add(Bytes.toBytes("book"), Bytes.toBytes("downloadCounter"), Bytes.toBytes(0L));

			doc.addField("id", indexed.getRandom());
			doc.addField("fileName", fileName);
			doc.addField("title", indexed.getTitle());
			doc.addField("author", indexed.getAuthor());
			doc.addField("pageNumber", indexed.getnPage());
			doc.addField("creationDate", indexed.getCreationDate());
			doc.addField("producer", indexed.getProducer());
			doc.addField("producer", indexed.getFileName());
			doc.addField("content", allTogether);
			// hConn.config.writeXml(System.out); - check configs.
			hTable.put(p);
			solr.add(doc);
			solr.commit();
			System.out.println("--------\nALERT: One book has been indexed.\nCreated random id is: "
					+ indexed.getRandom() + "\n--------");
			
			// obj.moveFile(file, new File("FolderName", fileName));// example of moving a file to a folder.
		}
		solr.close();
		hTable.close();
		return ok(uploadPost.render());
	}

	public static Result logout() {
		session("username", "-1");
		Date d = new Date();
		SimpleDateFormat f = new SimpleDateFormat("dd/MM/YY hh:mm:ss");
		System.out.println("Logout Time: " + f.format(d));
		return redirect(routes.Application.login());
	}
	
	public static void getPendingList(Contribution cc, Scan scan) throws IOException {

		HTable hTable = new HTable(config, "contribute");
		
		scan = new Scan();
		scan.addColumn(Bytes.toBytes("book"), Bytes.toBytes("author"));
		scan.addColumn(Bytes.toBytes("book"), Bytes.toBytes("name"));
		scan.addColumn(Bytes.toBytes("book"), Bytes.toBytes("producer"));
		scan.addColumn(Bytes.toBytes("book"), Bytes.toBytes("title"));
		scan.addColumn(Bytes.toBytes("book"), Bytes.toBytes("senderEmail"));
		org.apache.hadoop.hbase.client.ResultScanner scanner = hTable.getScanner(scan);
		for (org.apache.hadoop.hbase.client.Result result = scanner.next(); result != null; result = scanner
				.next()) {
			NavigableMap familyMap = result.getFamilyMap(Bytes.toBytes("book"));
			byte[] id = (byte[]) result.getRow();
			byte[] author = (byte[]) familyMap.get(Bytes.toBytes("author"));
			byte[] name = (byte[]) familyMap.get(Bytes.toBytes("name"));
			byte[] producer = (byte[]) familyMap.get(Bytes.toBytes("producer"));
			byte[] title = (byte[]) familyMap.get(Bytes.toBytes("title"));
			byte[] senderEmail = (byte[]) familyMap.get(Bytes.toBytes("senderEmail"));
			cc = new Contribution();
			cc.setId(Bytes.toString(id));
			cc.setAuthor(Bytes.toString(author));
			cc.setName(Bytes.toString(name));
			cc.setProducer(Bytes.toString(producer));
			cc.setTitle(Bytes.toString(title));
			cc.setEmail(Bytes.toString(senderEmail));
			contributeList.add(cc);
		}
	}

	public static Result adminPanel() throws IOException {
		contributeList.clear();
		Contribution cc = new Contribution();
		Scan scan = new Scan();
		if (null == session("username") || "-1".equals(session("username"))) {
			return ok(login.render(userForm));
		} else {
			getPendingList(cc, scan);
			return ok(adminPanel.render(cc, contributeList));
		}

	}

	public static Result contribute() {
		return ok(contribute.render(contributeForm));
	}

	public static Result uploadCPost() throws IOException {
		config.get("hbase.client.keyvalue.maxsize");
		config.set("hbase.client.keyvalue.maxsize", "-1");
		contributeList.clear();
		Form<Contribution> filledForm = contributeForm.bindFromRequest();
		Contribution cc = new Contribution();
		try {
			cc = filledForm.get();
		} catch (Exception e) {
			return notFound(
					"<h1>Something went wrong! Please try again.</h1><br><button onclick='goBack()'>Go Back</button><script>function goBack() { window.history.back();}</script>")
							.as("text/html");
		}
		HTable hTable = new HTable(config, "contribute");

		String id = UUID.randomUUID().toString();
		String title, author, producer = "";
		Put p = new Put(Bytes.toBytes(id));
		List<FilePart> fileParts = request().body().asMultipartFormData().getFiles();
		for (FilePart pdf : fileParts) {
			System.out.println(pdf.getContentType());
			String fileName = pdf.getFilename();
			final int lastPeriodPos = fileName.lastIndexOf('.');
			if (!(lastPeriodPos <= 0))
				fileName = fileName.substring(0, lastPeriodPos);

			File file = pdf.getFile();
			PDDocument document = null; // ova dopusti samo application/pdf type. Ne treba provjeriti content type.
			try{
			document = PDDocument.load(file, ""); // ako dokument nije .pdf, pokazi notFound();
			} catch (Exception e){
				return notFound(
						"<h1>Something went wrong! Please try again.</h1><br><button onclick='goBack()'>Go Back</button><script>function goBack() { window.history.back();}</script>")
								.as("text/html");
			}
			if (!document.equals(null) || !cc.getEmail().equals(null)) {
				PDDocumentInformation info = document.getDocumentInformation();
				title = info.getTitle();
				author = info.getAuthor();
				producer = info.getProducer();

				if (author == null)
					author = "noAuthor"; // *
				if (producer == null)
					producer = "noProducer";// *
				if (title == null)
					title = "noTitle"; // *

				FileInputStream fin = new FileInputStream(file);
				p.add(Bytes.toBytes("book"), Bytes.toBytes("name"), Bytes.toBytes(fileName));
				p.add(Bytes.toBytes("book"), Bytes.toBytes("title"), Bytes.toBytes(title));
				p.add(Bytes.toBytes("book"), Bytes.toBytes("author"), Bytes.toBytes(author));
				p.add(Bytes.toBytes("book"), Bytes.toBytes("producer"), Bytes.toBytes(producer));
				p.add(Bytes.toBytes("book"), Bytes.toBytes("document"), IOUtils.toByteArray(fin));
				p.add(Bytes.toBytes("book"), Bytes.toBytes("senderEmail"), Bytes.toBytes(cc.getEmail()));

				hTable.put(p);
				fin.close();
				hTable.close();
				try {
					String hostName = Play.application().configuration().getString("email.host");
					String userName = Play.application().configuration().getString("email.user");
					String password = Play.application().configuration().getString("email.password");
					HtmlEmail email = new HtmlEmail();
					email.setHostName(hostName);
					email.setSmtpPort(465);
					email.setAuthenticator(new DefaultAuthenticator(userName, password));
					email.setSSLOnConnect(true);
					email.setStartTLSEnabled(true);
					email.setFrom("contribute@riba.com");
					email.setSubject("Contribution Alert");
					email.setHtmlMsg(cc.getEmail() + " has contributed a book.<br> File name: " + fileName
							+ "  <br> Title: " + title + " <br> Author: " + author + " <br> Producer: " + producer);
					email.addTo("can1903ali@gmail.com");
					email.send();
					System.out.println("Sent");
				} catch (EmailException e) {
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
				return ok(uploadCPost.render());
			}
		}
		return play.mvc.Results
				.notFound(
						"<h1>Fill out the form!.</h1><br><button onclick='goBack()'>Go Back</button><script>function goBack() { window.history.back();}</script>")
				.as("text/html");

	}
	
	public static void deleteFromSolr(String id) throws SolrServerException, IOException {
		solrConfiguration();
		solr.deleteById(id);
		solr.commit();
		solr.close();

	}
	
	public static void deleteFromHBase(String id, HTable hTable) throws IOException {
		List<Delete> list = new ArrayList<Delete>();
		Delete d = new Delete(id.getBytes());
		list.add(d);
		hTable.delete(list);
		hTable.close();
	}

	public static Result delete(String id) throws IOException, SolrServerException {
		// moze try catch ili throws
		HTable hTableBook = new HTable(config, "books");
		deleteFromSolr(id);
		deleteFromHBase(id, hTableBook);

		return redirect("/");
	}

	public static Result deleteContribute(String id) throws IOException {
		HTable hTable = new HTable(config, "contribute");
		deleteFromHBase(id, hTable);
		return redirect("/adminPanel");
	}

	public static Result downloadContribute(String id) throws IOException {
		HTable hTable = new HTable(config, "contribute");
		Get g = new Get(Bytes.toBytes(id));
		g.addColumn(Bytes.toBytes("book"), Bytes.toBytes("document"));
		g.addColumn(Bytes.toBytes("book"), Bytes.toBytes("name"));

		org.apache.hadoop.hbase.client.Result result = hTable.get(g);
		if (result.containsColumn(Bytes.toBytes("book"), Bytes.toBytes("document"))) {
			byte[] rawBook = result.getNoVersionMap().get(Bytes.toBytes("book")).get(Bytes.toBytes("document")); // 1.
																													// family,
																													// 2.qualify
			byte[] fileName = result.getNoVersionMap().get(Bytes.toBytes("book")).get(Bytes.toBytes("name"));
			String fn = new String(fileName, "UTF-8");
			response().setContentType("application/octet-stream");
			response().setHeader("Content-disposition", "attachment; filename=" + fn + ".pdf");
			return ok(rawBook);
		}
		hTable.close();
		return play.mvc.Results
				.notFound(
						"<h1>Oops... Something went wrong!</h1><br><button onclick='goBack()'>Go Back</button><script>function goBack() { window.history.back();}</script>")
				.as("text/html");
	}
}