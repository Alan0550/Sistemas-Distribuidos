/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.upb.tickmaster.httpserver;

/**
 *
 * @author rlaredo
 */
public enum ContentType {
	CSS("CSS"), //
	GIF("GIF"), //
	HTM("HTM"), //
	HTML("HTML"), //
	ICO("ICO"), //
	JPG("JPG"), //
	JPEG("JPEG"), //
	PNG("PNG"), //
	TXT("TXT"), //
	XML("XML"),//
	JSON("JSON"); //

	private final String extension;

	ContentType(String extension) {
		this.extension = extension;
	}

	@Override
	public String toString() {
		switch (this) {
			case CSS:
				return "Content-Type: text/css";
			case GIF:
				return "Content-Type: image/gif";
			case HTM:
			case HTML:
				return "Content-Type: text/html";
			case ICO:
				return "Content-Type: image/gif";
			case JPG:
			case JPEG:
				return "Content-Type: image/jpeg";
			case PNG:
				return "Content-Type: image/png";
			case TXT:
				return "Content-type: text/plain";
			case XML:
				return "Content-type: text/xml";
			case JSON:
				return "application/json";
			default:
				return null;
		}
	}
}
