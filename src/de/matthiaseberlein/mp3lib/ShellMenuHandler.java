package de.matthiaseberlein.mp3lib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ShellMenuHandler {
	String[] options;
    int maxlength;
	
	public ShellMenuHandler(String ueberschrift, String...options){
		this.options = new String[options.length+1];
		this.options[0] = ueberschrift;
		System.arraycopy(options, 0, this.options, 1, options.length);
		maxlength=0;
		for(int i=1;i<this.options.length;i++) {
			this.options[i]=i+") "+this.options[i];
		}
		for(String option: this.options) {
			if(option.length()>maxlength)
				maxlength=option.length();
		}
	}
	
	public void printAllOptions(){ //Wird nach jeder durchgefuehrten Aktion neu aufgerufen
		for(int i=0;i<maxlength+6;i++) {
			if(i<maxlength+6-1) {
				if(i==0) {
					System.out.print("+");
				}
				else
					System.out.print("-");
			}
			else
				System.out.println("+");
		}

		System.out.println(addWall(options[0], maxlength+4+2, 2, "|"));

		for(int i=0;i<maxlength+6;i++) {
			if(i<maxlength+6-1) {
				if(i==0) {
					System.out.print("+");
				}
				else
					System.out.print("-");
			}
			else
				System.out.println("+");
		}

		for(int i=1;i<options.length;i++) {
			System.out.println(addWall(options[i], maxlength+6, 2, "|"));
		}
		
		for(int i=0;i<maxlength+6;i++) {
			if(i<maxlength+6-1) {
				if(i==0) {
					System.out.print("+");
				}
				else
					System.out.print("-");
			}
			else
				System.out.println("+");
		}
	}
    
	public int getUserChoice()throws IOException{ //Abfrage der Entscheidung des Users
		
		int choice = -1;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		do{
			printAllOptions();
			choice = Integer.parseInt(br.readLine());
		} while (choice < 0 || choice > this.options.length);
		
		if(choice == 0) System.out.println("JDBC beendet.");
		return choice;
		
	}	
	private String addWall(String s, int maxWidth, int distanceToWall, String wall){    //Trump would be happy to have this method
        String ausg="";
        if(s.length()>(maxWidth-(distanceToWall*2)-(wall.length()*2))){
            if(s.lastIndexOf(" ")==-1){
                s=s.substring(0, maxWidth-(distanceToWall*2)-(wall.length()*2)-5)+"[...]";
            }
        }
        ausg=wall;
        for(int i=0;i<distanceToWall;i++){
            ausg+=" ";
        }
        ausg+=s;
        //Fill up the space until the maxwidth-wall-spacing is reached.
        int filling=maxWidth-ausg.length()-distanceToWall-wall.length();
        for(int i=0;i<filling;i++){
            ausg+=" ";
        }
        for(int i=0;i<distanceToWall;i++){
            ausg+=" ";
        }
        ausg+=wall;

        return ausg;
    }
}