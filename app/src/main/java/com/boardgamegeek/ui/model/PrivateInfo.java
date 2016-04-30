package com.boardgamegeek.ui.model;

public class PrivateInfo {
	private String priceCurrency;
	private double price;
	private String currentValueCurrency;
	private double currentValue;
	private int quantity;
	private String acquiredFrom;
	private String acquisitionDate;
	private String privateComment;

	public String getPriceCurrency() {
		return priceCurrency;
	}

	public void setPriceCurrency(String priceCurrency) {
		this.priceCurrency = priceCurrency;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public String getCurrentValueCurrency() {
		return currentValueCurrency;
	}

	public void setCurrentValueCurrency(String currentValueCurrency) {
		this.currentValueCurrency = currentValueCurrency;
	}

	public double getCurrentValue() {
		return currentValue;
	}

	public void setCurrentValue(double currentValue) {
		this.currentValue = currentValue;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public String getAcquiredFrom() {
		return acquiredFrom;
	}

	public void setAcquiredFrom(String acquiredFrom) {
		this.acquiredFrom = acquiredFrom;
	}

	public String getAcquisitionDate() {
		return acquisitionDate;
	}

	public void setAcquisitionDate(String acquisitionDate) {
		this.acquisitionDate = acquisitionDate;
	}

	public String getPrivateComment() {
		return privateComment;
	}

	public void setPrivateComment(String privateComment) {
		this.privateComment = privateComment;
	}
}