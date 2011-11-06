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
public class Section {
    private Corpus corpus;
    private Sentence title;
    private ArrayList<Sentence> sentences;
    
    public Section(Corpus corpus, Sentence title){
        this.corpus = corpus;
        this.title = title;
        this.sentences = new ArrayList<Sentence>();
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
    
    public void addSentence(Sentence s){
        sentences.add(s);
    }
    
    public void setSentence(int i, Sentence s){
        sentences.set(i, s);
    }
    
    public Sentence getSentence(int i){
        return sentences.get(i);
    }
    
    public int size(){
        return sentences.size();
    }
    
    /**
     * Remove all the annotations of the section.
     */
    public void cleanAnnotations() {        
        for (int i = 0; i < size(); i++) {
            getSentence(i).cleanAnnotations();
        }
    }
}
