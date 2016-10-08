package controllers;

import java.io.File;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;

import models.Index;

///////////////
import play.*;
import play.Play;
///////////////

//Works in Terminal if you set necessary classpaths.
//Bulk /path/of/the/pdf/folder
public class Bulk {

	static HttpSolrClient solr = null;
	static Configuration config = HBaseConfiguration.create();
	static String namePath = "//home//alican//Desktop//names//111113.txt";
	// to add random name or producer if any of them are empty.
	
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
	
	public static void main(String[] args) throws IOException, SolrServerException {
		File folder = new File(args[0]);
		List<String> lines = FileUtils.readLines(new File(namePath), "utf-8");
		Random r = new Random();
		int i = 1;
		Index indexed = new Index();
		config.get("hbase.client.keyvalue.maxsize");
		config.set("hbase.client.keyvalue.maxsize", "-1");
		byte[] byteImage = null;
		File[] fileParts = folder.listFiles();
		HTable hTable = new HTable(config, "books");
		for (File pdf : fileParts) {
			System.out.println(i + ". " + pdf.getName());
			String fileName = pdf.getName();
			final int lastPeriodPos = fileName.lastIndexOf('.');
			if (!(lastPeriodPos <= 0))
				fileName = fileName.substring(0, lastPeriodPos);

			FileUtils obj = new FileUtils();
			
			PDDocument document = null;
			PDFTextStripper pdfstripper = null;
			PDDocumentInformation info = null;
			
			try{
			document = PDDocument.load(pdf, "");
			pdfstripper = new PDFTextStripper();
			info = document.getDocumentInformation();
			} catch (Exception e){
				System.out.println(pdf.getName() + " is not a PDF document!" );
			}
			
			indexed.setnPage(Integer.toString(document.getNumberOfPages()));
			indexed.setTitle(info.getTitle());
			indexed.setAuthor(info.getAuthor());
			indexed.setContent(pdfstripper.getText(document));
			indexed.setCreationDate(getProperDate(info.getCreationDate()));
			indexed.setProducer(info.getProducer());
			indexed.setFileName(fileName);

			//////////////////////////////////////////////////////////////
			//////// CHANGE THIS PART WHEN PROJECT IS DONE //////
			String gAuthor = indexed.getAuthor();
			String gProducer = indexed.getProducer();
			String gTitle = indexed.getTitle();
			if (gAuthor == null) {
				int item = r.nextInt(lines.size());
				indexed.setAuthor(lines.get(item));// *
			}
			if (gProducer == null) {
				int item = r.nextInt(lines.size());
				indexed.setProducer(lines.get(item) + " Production");// *
			}
			// *
			if (gTitle == null)
				indexed.setTitle("NoTitle"); // *
			//////////////////////////////////////////////////////////////


			solr = new HttpSolrClient("http://localhost:8983/solr/senior");
			// we don't use play libraries in this class. Hence we wrote direct solr link.
			SolrInputDocument doc = new SolrInputDocument();

			Set<String> indexedContentSet = new HashSet<>();
			Set<String> set = new HashSet<>();

			String arr[] = indexed.getContent().split("\\s+");
			for (String y : arr) {

				if (y.length() > 4) {

					if (set.contains(y)) {
						indexedContentSet.add(y.toLowerCase());
					}

					set.add(y.toLowerCase()); 
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
						document.setAllSecurityToBeRemoved(true);
					} catch (Exception e) {
						throw new Exception("cannot be decrypted. ", e);
					}
				}
				BufferedImage image = new BufferedImage(200, 150, BufferedImage.TYPE_3BYTE_BGR);
				image = pdfRenderer.renderImageWithDPI(0, 50);
				java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
				ImageIO.write(image, "png", bos);
				byteImage = bos.toByteArray();
				String newS = new String(byteImage, "UTF-8");

				document.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			FileInputStream fin = new FileInputStream(pdf);
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
			hTable.put(p);
			solr.add(doc);
			solr.commit();
			i++;
			System.out.println("--------\nALERT: One book has been indexed.\nCreated random id is: "
					+ indexed.getRandom() + "\n--------");
		}
		solr.close();
		hTable.close();
System.out.println("INsertion is done.");
	}
}
