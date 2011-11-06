/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.tm.gimli.corpus;

import java.util.ArrayList;

/**
 *
 * @author david
 */
public class Document {
    private Corpus corpus;
    private Sentence title;
    private ArrayList<Section> sections;
    
    public Document(Corpus corpus, Sentence title){
        this.corpus = corpus;
        this.title = title;
        this.sections = new ArrayList<Section>();
    }
    
    public Corpus getCorpus() {
        return corpus;
    }

    public Sentence getTitle() {
        return title;
    }

    public void setTitle(Sentence title) {
        this.title = title;
    }
    
    public void addSection (Section s){
        sections.add(s);
    }
    
    public void setSection (int i, Section s){
        sections.set(i, s);
    }
    
    public Section getSection (int i){
        return sections.get(i);
    }
    
    public int size(){
        return sections.size();
    }
    
    /**
     * Remove all the annotations of the document.
     */
    public void cleanAnnotations() {        
        for (int i = 0; i < size(); i++) {
            getSection(i).cleanAnnotations();
        }
    }
}
