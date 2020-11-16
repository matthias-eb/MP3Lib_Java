package de.matthiaseberlein.mp3lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ID3Reader{
	public static void main(String args[]) throws IOException {
		Frame.init();
		String eing="";
		File f=null;
		if(args.length==0) {
			System.out.println("Willkommen zum ID3Reader. geben sie eine Datei ein.");
			BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
			boolean wiederholen=false;
			do{
				wiederholen=false;
				eing=br.readLine();
				if(!eing.endsWith(".mp3"))
					wiederholen=true;
				else {
					f=new File(eing);
					if(!f.exists())
						wiederholen=true;
				}
				if(wiederholen)
					System.out.println("Bitte geben sie eine mp3 Datei an. Wenn diese Datei nicht im selben Ordner liegt, wie diese Programm, muss der komplette Pfad angegeben werden.");
			} while(wiederholen);
		}
		else{
			f=new File(args[0]);
			if(!f.exists() || !args[0].endsWith(".mp3")) {
				System.out.println("Ungültige Datei angegeben. Beende..");
				return;
			}
		}
		System.out.println("Starte ID3Reader...");

		Song song=new Song(f.getAbsolutePath());
		song.showTagRepresentation();
		System.out.println(song.toString());

		boolean cont=true;
		int auswahl=0;
		ShellMenuHandler menu;
		do{
			menu=new ShellMenuHandler("Menü", "Informationen bearbeiten", "Informationen setzen", "Informationen löschen", "Informationsteil hinzufügen", "Informationsteil löschen", "Programm beenden");
			auswahl=menu.getUserChoice();
			Object[] frorderobjs;
			String[] frameorder;
			InformationHolder holder, holder2;
			InformationHolder.Information info;
			boolean zwischenstep;
			int ausw2, ausw3;
			ArrayList<String> infoList;
			switch(auswahl) {
				case 1:
					frorderobjs = song.getCurrentFrameOrder().toArray();
					frameorder= new String[frorderobjs.length+1];
					for(int i=0;i<frorderobjs.length; i++) {
						frameorder[i]= (String) frorderobjs[i];
					}
					frameorder[frameorder.length-1]="Abbrechen";
					menu=new ShellMenuHandler("Menue - Information bearbeiten", frameorder);
					ausw2=menu.getUserChoice();
					if(ausw2==frameorder.length)
						break;
					
					//Hole die gewählte Information aus dem Song
					holder=song.get(frameorder[ausw2-1]);
					zwischenstep=false;
					infoList=new ArrayList<>();
					//Schaue, ob es eine zwischenebene Gibt, weil es mehrere Kommentare und ähnliches geben kann
					if(!holder.contains(frameorder[ausw2-1])){
						zwischenstep=true;
					}
					//Fülle die Liste zum auswählen im Menü erneut
					for(InformationHolder.Information infox: holder) {
						infoList.add(infox.getKey());
					}
					infoList.add("Abbrechen");
					holder2=new InformationHolder();
					if(zwischenstep) {
						menu = new ShellMenuHandler("Menü - " + frameorder[ausw2 - 1] + " auswählen", infoList.toArray(new String[0]));
						ausw3=menu.getUserChoice();
						if (ausw3 == infoList.size())
							break;
						String frameID=infoList.get(ausw3-1);
						holder2 = holder.getHolder(infoList.get(ausw3 - 1));
						infoList.clear();
						for (InformationHolder.Information infox : holder2) {
							infoList.add(infox.getKey());
						}
						infoList.add("Abbrechen");
						menu = new ShellMenuHandler("Menü - "+frameorder[ausw2 -1]+" "+frameID+" bearbeiten", infoList.toArray(new String[0]));
						ausw3=menu.getUserChoice();
					} else {
						holder2=holder.getHolder(frameorder[ausw2-1]);
						menu = new ShellMenuHandler("Menü - " + frameorder[ausw2 - 1] + " bearbeiten", infoList.toArray(new String[0]));
						ausw3=menu.getUserChoice();
					}
					if(ausw3==infoList.size())
						break;
					System.out.println("Geben sie einen neuen Wert ein:");
					BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
					String key=infoList.get(ausw3-1);
					holder2.setPosition(0);
					Class innerClass=null;
					while(holder2.hasNext()) {
						info=holder2.next();
						if (info.getKey().equals(key))
							innerClass=info.getInnerClass();
					}
					holder2.remove(key);
					if(String.class.isAssignableFrom(innerClass)) {
						holder2.with(key, br.readLine());
						System.out.println(holder.toString());
						System.out.println(holder2.toString());
					}
					else if(Comment.class.isAssignableFrom(innerClass)){
						System.out.println("Kommentar eingeben: ");
						String comment=br.readLine();
						System.out.println("Zusammenfassung eingeben: ");
						String content_descr=br.readLine();
						System.out.println("Geben sie eine Sprache ein: ");
						LanguageCode lang=LanguageCode.getLanguageCodeByValue(br.readLine());
						Comment comm=new Comment(comment, content_descr, lang);
						System.out.println("Test: "+comm.toString());
						holder2.with(key, comm);
					}
					else if(Image.class.isAssignableFrom(innerClass)){
						System.out.println("Das Bild selber lässt sich momentan nicht ändern.");
					}
					else if(Integer.class.isAssignableFrom(innerClass)){
						boolean contin;
						do {
							contin=true;
							try {
								holder2.with(key, Integer.parseInt(br.readLine()));
							} catch(NumberFormatException nfe){
								contin=false;
								System.out.println("Bitte geben sie eine ganze Zahl ein.");
							}
						} while(!contin);
					}
					else if(ArrayList.class.isAssignableFrom(innerClass)){
						ArrayList<String> list=new ArrayList<>();
						do{
							System.out.println("Wenn sie fertig sind, drücken sie Enter bei einer leeren Zeile.");
							String s=br.readLine();
							if(s.equals("")){
								break;
							}
							list.add(s);
						} while(true);
						holder2.with(key, list);
					}
					else{
						System.out.println("Klasse: "+innerClass.toString());
						System.out.println("Holder: "+holder.toString());
					}
					System.out.println("Erfolg beim Setzen des neuen wertes? "+song.setData(new InformationHolder().with(frameorder[ausw2-1], holder)));
					break;
				case 2:
					menu=new ShellMenuHandler("Information auswählen", "TITEL", "ALBUM", "INTERPRETEN", "Titelnummer", "Kommentar", "Jahr", "Genres", "Inhaltstyp", "Position im Song", "Abbrechen");
					int ausw=menu.getUserChoice();
					BufferedReader br2=new BufferedReader(new InputStreamReader(System.in));
					String s=null;
					switch(ausw){
						case 1:
							s=song.getTitle();
							if(s!=null)
								System.out.println("Bisheriger Titel: "+s);
							System.out.println("Geben sie einen Titel ein: ");
							System.out.println("Erfolg? "+song.setTitle(br2.readLine()));
							break;
						case 2:
							s=song.getAlbum();
							if(s!=null)
								System.out.println("Bisheriges Album: "+s);
							System.out.println("Geben sie ein neues Album ein: ");
							System.out.println("Erfolg? "+song.setAlbum(br2.readLine()));
							break;
						case 3:
							ArrayList<String> interpreten=song.getArtists();
							if(interpreten!=null){
								System.out.println("Bisherige Interpreten:");
								for(String x : interpreten){
									System.out.println("\t"+x);
								}
								interpreten.clear();
							}
							else
								interpreten=new ArrayList<>();
							System.out.println("Geben sie neue Interpreten ein, und drücken sie beim letzten zwei Mal Enter.");
							do {
								s=br2.readLine();
								if(s.equals(""))
									break;
								else
									interpreten.add(s);
							} while(true);
							System.out.println("Erfolg? "+song.setArtists(interpreten));
							break;
						case 4:
							System.out.println("Geben sie eine neue Titelnummer ein.");
							int val=song.getTracknmbr();
							int val2=song.getTotalTracks();
							int trcknmbr=-1;
							int total_tracks=-1;
							if(val!=-1){
								System.out.println("Bisherige Nummer: "+val);
								System.out.println("Bisherige maximale Tracknummer: ");
							}
							
							boolean erfolg=true;
							do {
								erfolg = true;
								try {
									System.out.println("Geben sie eine neue Titelnummer ein. Bisherige Titelnummer: "+val);
									trcknmbr = Integer.parseInt(br2.readLine());
								} catch (NumberFormatException nfe) {
									System.out.println("Geben sie eine positive Zahl ein.");
									erfolg = false;
								}
							}while(!erfolg);
							do {
								erfolg = true;
								try {
									System.out.println("Gesamtanzahl der Titel: "+val2);
									total_tracks = Integer.parseInt(br2.readLine());
								} catch (NumberFormatException nfe) {
									System.out.println("Geben sie eine positive ganze Zahl ein.");
									erfolg = false;
								}
							}while(!erfolg);
							System.out.println("Erfolg? "+song.setTracknmbr(trcknmbr, total_tracks));
							break;
						case 5:
							Comment c=null;
							String oldIdentifier=null;
							System.out.println("Geben sie einen Kommentar ein.");
							String com=br2.readLine();
							System.out.println("Geben sie eine Zusammenfassung ein.");
							String cont_t=br2.readLine();
							System.out.println("Geben sie nun eine Sprache ein.");
							LanguageCode lc=null;
							do{
								lc=LanguageCode.getLanguageCodeByValue(br2.readLine());
								if(lc==null)
									System.out.println("Geben sie eine Sprache wie z.B. Deutsch oder German oder English ein.");
							} while(lc==null);
							c=new Comment(com, cont_t, lc);
							System.out.println("Erfolg? "+song.replaceComment(oldIdentifier, c));
							break;
						case 6:
							System.out.println("Geben sie ein Jahr ein.");
							int year=-1;
							do {
								try {
									year=Integer.parseInt(br2.readLine());
									break;
								} catch (NumberFormatException nfe) {
									System.out.println("Das ist keine ganze Zahl.");
								}
							} while(true);
							System.out.println("Erfolg? "+song.setYear(year));
							break;
						case 7:
							ArrayList<String> l=new ArrayList<>();
							System.out.println("Geben sie Genres ein bis sie zufrieden sind und drücken sie dann noch einmal Enter.");
							do{
								String s1=br2.readLine();
								if(s1.equals("")){
									break;
								}
								l.add(s1);
							}while(true);
							System.out.println("Erfolg? "+song.setGenres(l));
							break;
						case 8:
							s=song.getContentGroup();
							if(s!=null)
								System.out.println("Bisherige Inhaltsgruppe: "+s);
							System.out.println("Geben sie eine InhaltsGruppe ein: ");
							System.out.println("Erfolg? "+song.setContentGroup(br2.readLine()));
							break;
						case 9:
							String[] tsformats=new String[FramePOSS.timestampformats.length+1];
							System.arraycopy(FramePOSS.timestampformats, 0, tsformats, 0, FramePOSS.timestampformats.length);
							menu=new ShellMenuHandler("In welcher Art wollen sie die neue Position eintragen?", tsformats);
							int auswx=menu.getUserChoice();
							System.out.println("Geben sie nun die Position ein.");
							int position=-1;
							do{
								try{
									position=Integer.parseInt(br2.readLine());
									break;
								} catch(NumberFormatException nfe){
								
								}
							} while(true);
							song.setPosition(position, tsformats[auswx-1]);
						default:
							break;
							
					}
					break;
				case 3:
					frorderobjs = song.getCurrentFrameOrder().toArray();
					frameorder= new String[frorderobjs.length+1];
					for(int i=0;i<frorderobjs.length; i++) {
						frameorder[i]= (String) frorderobjs[i];
					}
					frameorder[frameorder.length-1]="Abbrechen";
					menu=new ShellMenuHandler("Menue - Information löschen", frameorder);
					ausw2=menu.getUserChoice();
					if(ausw2==frameorder.length)
						break;
					
					//Hole die gewählte Information aus dem Song
					holder=song.get(frameorder[ausw2-1]);
					zwischenstep=false;
					infoList=new ArrayList<>();
					//Schaue, ob es eine zwischenebene Gibt, weil es mehrere Kommentare und ähnliches geben kann
					if(!holder.equals(frameorder[ausw2-1])){
						zwischenstep=true;
					}
					//Fülle die Liste zum auswählen im Menü erneut
					for(InformationHolder.Information infox: holder) {
						infoList.add(infox.getKey());
					}
					infoList.add("Alle entfernen");
					infoList.add("Abbrechen");
					
					String frameID;
					boolean remAll=false;
					if(zwischenstep) {
						//Zweck auswählen (1. Ebene)
						menu = new ShellMenuHandler("Menü - " + frameorder[ausw2 - 1] + " auswählen", infoList.toArray(new String[0]));
						ausw3=menu.getUserChoice();
						if (ausw3 == infoList.size())
							break;
						if(ausw3 == infoList.size()-1)
							remAll=true;
						frameID=infoList.get(ausw3-1);
					} else {
						frameID=frameorder[ausw2-1];
					}
					
					//Information löschen
					MetaID id=MetaID.valueOf(frameorder[ausw2-1]);
					switch (id){
						case COMMENT:
							if(remAll)
								System.out.println("Erfolg? "+song.removeAllComments());
							else
								System.out.println("Erfolg? "+song.removeComment(frameID));
							break;
						case TITLE:
							System.out.println("Erfolg? "+song.removeTitle());
							break;
						case CONDUCTOR:
							System.out.println("Erfolg? "+song.removeConductor());
							break;
						case CONTENT_GROUP:
							System.out.println("Erfolg? "+song.removeContentGroup());
							break;
						case ALBUM:
							System.out.println("Erfolg? "+song.removeAlbum());
							break;
						case SUBTITLE:
							System.out.println("Erfolg? "+song.removeSubtitle());
							break;
						case ARTISTS:
							System.out.println("Erfolg? "+song.removeAllArtists());
							break;
						case POSITION:
							System.out.println("Erfolg? "+song.removePosition());
							break;
						case PERFORMER_INFO:
							System.out.println("Erfolg? "+song.removePerformerInfo());
							break;
						case ENCODER:
							System.out.println("Erfolg? "+song.removeEncoder());
							break;
						case BPM:
							System.out.println("Erfolg? "+song.removeBPM());
							break;
						case IMAGE:
							if(remAll)
								System.out.println("Erfolg? "+song.removeAllImages());
							else
								System.out.println("Erfolg? "+song.removeImage(frameID));
							break;
						case TRACK_NUMBER:
							System.out.println("Erfolg? "+song.removeTracknmbr());
							break;
						case DATE:
							System.out.println("Erfolg? "+song.removeDate());
							break;
							
						default:
					}
					break;
				default:
					cont=false;
					break;
			}
			System.out.println(song.toString());
			song.showTagRepresentation();
		} while(cont);
	}
}