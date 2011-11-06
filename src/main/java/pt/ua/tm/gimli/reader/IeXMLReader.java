/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.gimli.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.tm.gimli.config.Constants.EntityType;
import pt.ua.tm.gimli.config.Constants.LabelFormat;
import pt.ua.tm.gimli.corpus.Annotation;
import pt.ua.tm.gimli.corpus.Corpus;
import pt.ua.tm.gimli.corpus.Sentence;
import pt.ua.tm.gimli.corpus.Token;
import pt.ua.tm.gimli.exception.GimliException;
import pt.ua.tm.gimli.external.gdep.GDepCorpus;
import pt.ua.tm.gimli.external.gdep.GDepParser;
import pt.ua.tm.gimli.external.gdep.GDepSentence;
import pt.ua.tm.gimli.external.gdep.GDepToken;

/**
 *
 * @author david
 */
public class IeXMLReader implements ICorpusReader {

    /**
     * {@link Logger} to be used in the class.
     */
    private static Logger logger = LoggerFactory.getLogger(IeXMLReader.class);
    private static String SENTENCE = "s";
    private static String PMID = "PMID";
    private static String ANNOTATION = "e";
    private static String TITLE = "ArticleTitle";
    private static String ABSTRACT = "AbstractText";
    private static String ID = "id";
    private static String CATEGORY = "sub";
    private String fileCorpus;
    private String fileGDep;

    public IeXMLReader(final String fileCorpus, final String fileGDep) {
        this.fileCorpus = fileCorpus;
        this.fileGDep = fileGDep;
    }

    /**
     * Get the number of characters of a {@link String}, discarding white space
     * characters.
     * @param t The text to be analyzed.
     * @return The number of characters.
     */
    private static int getCharSize(String t) {
        String regex = "[\\s]";

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(t);

        int count = 0;
        while (m.find()) {
            count++;
        }

        return t.length() - count;
    }

    public static void main(String[] args) {

        IeXMLReader reader = new IeXMLReader("/Users/david/Downloads/FSU_PRGE/all.xml", "/Users/david/Desktop/gdep_FSU.gz");
        
        //IeXMLReader reader = new IeXMLReader("/Users/david/Downloads/jnlpba.xml", "/Users/david/Desktop/gdep.gz");

        try {
            Corpus c = reader.read(LabelFormat.BIO);
            c.writeToFile("/Users/david/Desktop/corpus_FSU.gz");
        }
        catch (GimliException ex) {
            logger.error("Problem getting the GDep output.", ex);
        }
    }

    @Override
    public Corpus read(LabelFormat format) throws GimliException {
        // Load GDep data
        boolean gdepExists = new File(fileGDep).exists();
        GDepCorpus gdep = new GDepCorpus();

        if (fileGDep == null || !gdepExists) {
            logger.info("Running GDep parser");
            gdep = getGDepCorpus();
            logger.info("Saving GDep parsing result into file...");
            gdep.writeToFile(fileGDep);
        } else {
            logger.info("Loading GDep parsing from file...");
            gdep.loadFromFile(fileGDep);
        }

        Corpus c = loadCorpus(format, gdep);
        return c;
    }

    @Override
    public GDepCorpus getGDepCorpus() throws GimliException {
        GDepParser parser = new GDepParser(true);

        try {
            parser.launch();
        }
        catch (Exception ex) {
            throw new GimliException("Problem launching the GDep parser.", ex);
        }

        GDepCorpus corpus = new GDepCorpus();
        GDepSentence gs;

        boolean inSentence = false;
        String sentence = "";
        try {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            InputStream in = new FileInputStream(new File(fileCorpus));
            XMLEventReader eventReader = inputFactory.createXMLEventReader(in);

            // Read XML document
            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.isStartElement()) {
                    StartElement startElement = event.asStartElement();
                    if (startElement.getName().getLocalPart().equals(SENTENCE)) {
                        inSentence = true;
                        sentence = "";
                    }
                }

                if (inSentence) {
                    if (event.isCharacters()) {
                        sentence += event.asCharacters().getData();
                    }
                }

                if (event.isEndElement()) {
                    EndElement endElement = event.asEndElement();
                    if (endElement.getName().getLocalPart().equals(SENTENCE)) {
                        
                        // Unescape XML symbols
                        sentence = StringEscapeUtils.unescapeXml(sentence);
                        
                        // Customize tokenisation
                        sentence = sentence.replaceAll("/", " / ");
                        sentence = sentence.replaceAll("-", " - ");
                        sentence = sentence.replaceAll("[.]", " . ");
                        sentence = sentence.replaceAll("//s+", " ");
                        
                        gs = getGDepSentence(parser, corpus, sentence);
                        corpus.addSentence(gs);
                        inSentence = false;
                    }
                }
            }
        }
        catch (Exception ex) {
            throw new GimliException("A problem occured while parsing the XML file.", ex);
        }

        parser.terminate();
        return corpus;
    }

    private GDepSentence getGDepSentence(GDepParser gdep, GDepCorpus gcorpus, String sentence) throws GimliException {
        List<Object> list;
        GDepSentence s;
        String[] parts;
        String token, lemma, pos, chunk, depTag;
        int depToken;
        GDepToken t;

        list = gdep.parse(sentence);

        s = new GDepSentence(gcorpus);
        for (int i = 0; i < list.size(); i++) {

            parts = list.get(i).toString().split("\t");

            token = parts[1];
            token = token.replaceAll("''", "\"");
            token = token.replaceAll("``", "\"");

            lemma = parts[2];
            chunk = parts[3];
            pos = parts[4];
            depToken = Integer.valueOf(parts[6]) - 1;
            depTag = parts[7];

            t = new GDepToken(token, lemma, pos, chunk, depToken, depTag);
            s.addToken(t);
        }

        return s;
    }

    private Corpus loadCorpus(final LabelFormat format, final GDepCorpus gcorpus) throws GimliException {
        Corpus c = new Corpus(format, EntityType.protein);
        Sentence s = null;
        GDepSentence gs;
        int charStart, charEnd;
        Token t;
        GDepToken gt;

        // Parse GDep Output
        for (int i = 0; i < gcorpus.size(); i++) {
            gs = gcorpus.getSentence(i);
            s = new Sentence(c);

            charStart = 0;
            for (int k = 0; k < gs.size(); k++) {
                gt = gs.getToken(k);
                t = new Token(s, charStart, k, gs);
                charStart = t.getEnd() + 1;
                s.addToken(t);
            }
            c.addSentence(s);
        }


        int counter = 0;
        StringBuilder pmid = new StringBuilder();
        StringBuilder sid = new StringBuilder();
        StringBuilder pmid_sid;

        // Get Annotations
        boolean inSentence = false;
        StringBuilder sb = new StringBuilder();
        try {
            // First create a new XMLInputFactory
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();

            // Setup a new eventReader
            InputStream in = new FileInputStream(new File(fileCorpus));
            XMLEventReader eventReader = inputFactory.createXMLEventReader(in);

            // Read the XML document
            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.isStartElement()) {
                    StartElement startElement = event.asStartElement();

                    if (startElement.getName().getLocalPart().equals(PMID)) {
                        event = eventReader.nextEvent();
                        pmid = new StringBuilder();
                        pmid.append(event.asCharacters().getData());
                    }

                    if (startElement.getName().getLocalPart() == SENTENCE) {

                        // Get Sentence ID
                        Iterator<Attribute> attributes = startElement.getAttributes();
                        while (attributes.hasNext()) {
                            Attribute attribute = attributes.next();
                            if (attribute.getName().toString().equals(ID)) {
                                sid = new StringBuilder();
                                sid.append(attribute.getValue());
                            }
                        }
                        sb = new StringBuilder();
                        inSentence = true;

                        s = c.getSentence(counter);
                        pmid_sid = new StringBuilder(pmid);
                        pmid_sid.append("_");
                        pmid_sid.append(sid);
                        s.setId(pmid_sid.toString());

                        counter++;
                    }


                    if (startElement.getName().getLocalPart() == ANNOTATION) {
                        // Get Annotation ID
                        /*Iterator<Attribute> attributes = startElement.getAttributes();
                        while (attributes.hasNext()) {
                        Attribute attribute = attributes.next();
                        if (attribute.getName().toString().equals(ID)) {
                        logger.info("ANNOTATION ID: {}", attribute.getValue());
                        }
                        if (attribute.getName().toString().equals(CATEGORY)) {
                        logger.info("ANNOTATION CATEGORY: {}", attribute.getValue());
                        }
                        }*/

                        // Get Annotation Text and Start and Enc characters position
                        charStart = getCharSize(sb.toString()) + 1;

                        event = eventReader.nextEvent();
                        if (event.isCharacters()) {
                            sb.append(event.asCharacters().getData());
                        }

                        charEnd = getCharSize(sb.toString());

                        // Add annotation to sentence
                        addAnnotation(s, charStart, charEnd);
                        
                        event = eventReader.nextEvent();
                    }
                }

                if (inSentence) {
                    if (event.isCharacters()) {
                        sb.append(event.asCharacters().getData());
                    }
                }

                if (event.isEndElement()) {
                    EndElement endElement = event.asEndElement();
                    if (endElement.getName().getLocalPart() == SENTENCE) {
                        inSentence = false;
                    }
                }
            }
        }
        catch (Exception ex) {
            throw new GimliException("There was a problem parsing the XML filme.", ex);
        }

        return c;
    }

    private void addAnnotation(Sentence s, final int charStart, final int charEnd)
            throws GimliException {
        int charCount = 1;
        int tokenStart, tokenEnd;

        for (int i = 0; i < s.size(); i++) {
            if (charCount == charStart) {
                tokenStart = tokenEnd = i;
                for (int j = i; j < s.size() && charCount <= charEnd; j++) {
                    tokenEnd = j;
                    charCount += s.getToken(j).getText().length();
                }

                Annotation a = new Annotation(s, tokenStart, tokenEnd, 1.0);
                s.addAnnotation(a);

                return;
            }

            charCount += s.getToken(i).getText().length();
        }

        logger.error("SENTENCE: {}", s);
        logger.error("{}-{}", charStart, charEnd);
        
        /*throw new GimliException("The specified annotation does not exists"
                + " on the sentence.");*/
    }
}
