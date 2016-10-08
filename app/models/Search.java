package models;

import java.util.Date;

public class Search {

	public String getOutputId() {
		return outputId;
	}

	public void setOutputId(String outputId) {
		this.outputId = outputId;
	}

	public String getOutputTitle() {
		return outputTitle;
	}

	public void setOutputTitle(String outputTitle) {
		this.outputTitle = outputTitle;
	}

	public String getOutputAuthor() {
		return outputAuthor;
	}

	public void setOutputAuthor(String outputAuthor) {
		this.outputAuthor = outputAuthor;
	}

	public String getOutputContent() {
		return outputContent;
	}

	public void setOutputContent(String outputContent) {
		this.outputContent = outputContent;
	}

	public String getOutputPage() {
		return outputPage;
	}

	public void setOutputPage(String outputPage) {
		this.outputPage = outputPage;
	}

	public String getOutputCreationDate() {
		return outputCreationDate;
	}

	public void setOutputCreationDate(String string) {
		this.outputCreationDate = string;
	}

	public String getOutputProducer() {
		return outputProducer;
	}

	public void setOutputProducer(String outputProducer) {
		this.outputProducer = outputProducer;
	}

	public long getOutputDownloadCounter() {
		return outputDownloadCounter;
	}

	public void setOutputDownloadCounter(long outputDownloadCounter) {
		this.outputDownloadCounter = outputDownloadCounter;
	}

	public String getOutputPicture() {
		return outputPicture;
	}

	public void setOutputPicture(String outputPicture) {
		this.outputPicture = outputPicture;
	}

	public long getOutputFound() {
		return outputFound;
	}

	public void setOutputFound(long outputFound) {
		this.outputFound = outputFound;
	}

	private String outputId;
	private String outputTitle;
	private String outputAuthor;
	private String outputContent;
	private String outputPage;
	private String outputCreationDate;
	private String outputProducer;
	private long outputDownloadCounter;
	private String outputPicture;
	
	private long outputFound;
}