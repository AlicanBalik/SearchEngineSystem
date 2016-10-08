package models;

import java.util.Calendar;
import java.util.Date;

public class Index {
	
	private String random;
	public String getRandom() {
		return random;
	}
	public void setRandom(String random) {
		this.random = random;
	}
	///////////////////////////////////
	private String title;
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
///////////////////////////////////
	private String author;
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
///////////////////////////////////
	private String content;
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	
///////////////////////////////////
	private String nPage;
	public String getnPage() {
		return nPage;
	}
	public void setnPage(String nPage) {
		this.nPage = nPage;
	}
	
///////////////////////////////////
	private String creationDate; //Calendar and Date tested
	public String getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}
	
///////////////////////////////////
	private String producer;
	public String getProducer() {
		return producer;
	}
	public void setProducer(String producer) {
		this.producer = producer;
	}
///////////////////////////////////
	private String fileName;
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
///////////////////////////////////
	private String picture;
	public String getPicture() {
		return picture;
	}
	public void setPicture(String picture) {
		this.picture = picture;
	}
}
