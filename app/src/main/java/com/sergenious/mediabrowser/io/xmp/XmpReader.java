package com.sergenious.mediabrowser.io.xmp;

import com.sergenious.mediabrowser.io.JpegAppExtractor;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class XmpReader {
	public static final String NS_XAP = "http://ns.adobe.com/xap/1.0/";
	public static final String NS_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String NS_GPANO = "http://ns.google.com/photos/1.0/panorama/";
	
	public static Document extract(File file) throws IOException {
		List<Document> parsedDocuments = new ArrayList<>();

		boolean isJpeg = JpegAppExtractor.extract(file, (appName, appFileOfs, appStream, appStreamSize) -> {
			if (XmpReader.NS_XAP.equals(appName)) {
				try {
					String xmpData = JpegAppExtractor.readString(appStream, appStreamSize);
					DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
					builderFactory.setNamespaceAware(true);
					DocumentBuilder docBuilder = builderFactory.newDocumentBuilder();
					parsedDocuments.add(docBuilder.parse(new InputSource(new StringReader(xmpData))));
					return JpegAppExtractor.JpegAppReadState.READ_COMPLETED;
				}
				catch (Exception e) {
					throw new IOException("Error parsing XMP", e);
				}
			}
			return JpegAppExtractor.JpegAppReadState.UNREAD;
		});

		return (isJpeg && !parsedDocuments.isEmpty()) ? parsedDocuments.get(0) : null;
	}

	public static Element findRDF(Document xmpDoc) {
		if (xmpDoc == null) {
			return null;
		}
		NodeList nodes = xmpDoc.getElementsByTagNameNS(NS_RDF, "RDF");
		return (nodes != null) && (nodes.getLength() >= 1) ? (Element) nodes.item(0) : null;
	}
	
	public static Element findDescription(Element rdfElem, String namespace) {
		if (rdfElem == null) {
			return null;
		}
		NodeList nodes = rdfElem.getElementsByTagNameNS(NS_RDF, "Description");
		for (int i = 0; i < nodes.getLength(); i++) {
			Element descElem = (Element) nodes.item(i);
			NamedNodeMap attrs = descElem.getAttributes();
			for (int attrIndex = 0; attrIndex < attrs.getLength(); attrIndex++) {
				Attr attr = (Attr) attrs.item(attrIndex);
				if ("xmlns".equals(attr.getPrefix()) && namespace.equals(attr.getValue())) {
					return descElem;
				}
			}
		}
		return null;
	}
	
	public static String readString(Element descElem, String namespace, String key) {
		if (descElem == null) {
			return null;
		}
		String value = getChildText(getFirstChildElement(descElem, namespace, key));
		if (value != null) {
			return value;
		}

		return descElem.getAttributeNS(namespace, key);
	}

	public static Float readFloat(Element descElem, String namespace, String key, Float defaultValue) {
		try {
			String value = readString(descElem, namespace, key);
			return (value != null) ? Float.valueOf(value) : defaultValue;
		}
		catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static Element getFirstChildElement(Element parentElement, String ns, String name) {
		NodeList elements = parentElement.getElementsByTagNameNS(ns, name);
		return (elements.getLength() > 0) ? (Element) elements.item(0) : null;
	}

	public static String getChildText(Element element) {
		if (element == null) {
			return null;
		}
		for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node.getNodeType() == Node.TEXT_NODE) {
				return node.getNodeValue();
			}
			if (node.getNodeType() == Node.CDATA_SECTION_NODE) {
				return node.getNodeValue();
			}
		}
		return null;
	}
}
