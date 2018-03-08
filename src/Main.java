
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class Main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Start subprojects");
		try {
			List<String> lista = new ArrayList<String>();
			Class.forName("org.mariadb.jdbc.Driver");
			Connection conn=DriverManager.getConnection("jdbc:mariadb://192.168.90.123/fatdb","listy","listy1234");
			Statement st1 = conn.createStatement();
			ResultSet rs1 = st1.executeQuery("SELECT consumerorder FROM PartsOverview  WHERE OrderNo <> ConsumerOrder and ConsumerOrder not like '500%' group by OrderNo, ConsumerOrder");
			while(rs1.next()){
				lista.add(rs1.getString("ConsumerOrder"));
			}
			rs1.close();
			st1.close();
			for(String a : lista ){
				Statement st2 = conn.createStatement(); 
				st2.executeUpdate("Update PartsOverview set Description = 'subproject' where OrderNo = '"+a+"'");
				st2.close();
			}
			
			//konstrukcyjne
			/*Statement st3 = conn.createStatement();
			ResultSet rs3 = st3.executeQuery("SELECT * FROM PartsOverview where ItemNo like 'KM-%'");
			while(rs3.next()){
				String numerMaszyny = rs3.getString("OrderNo");
				String opis = rs3.getString("Description");
				String zadanie = rs3.getString("ItemNo");
				String opis2 = opis;
				if(numerMaszyny.length()>8)
					numerMaszyny = numerMaszyny.substring(0,8);
				if(opis.equals("subproject")){
					Statement st4 = conn.createStatement();
					ResultSet rs4 = st4.executeQuery("Select artikelomschrijving from bestellingdetail where (leverancier = '2' or leverancier = '6') and ordernummer ='"+numerMaszyny.substring(2, 8)+"'");
					while(rs4.next()){
						opis2 = rs4.getString(1);
					}
					rs4.close();
					st4.close();
				}
				Statement st5 = conn.createStatement();
				st5.executeUpdate("Update PartsOverview set OrderNo = '"+numerMaszyny+"', Description = '"+opis2+"' where ItemNo = '"+zadanie+"'");
				st5.close();
			}
			rs3.close();
			st3.close();
			*/
			
			//SPRAWDZENIE PODPROJEKTÓW!
			Statement st6 = conn.createStatement();
			ResultSet rs6 = st6.executeQuery("SELECT distinct orderno FROM PartsOverview  WHERE Description = 'subproject'");
			while(rs6.next()){
				String zamowienie = rs6.getString("OrderNo");
				String[] projekt = zamowienie.split("/");
				String nadrzedny1 = "";
				String nadrzedny2 = "";
				String nazwa = "";
				//wyszukaj projekt nadrzedny 
				Statement st7 = conn.createStatement();
				ResultSet rs7 = st7.executeQuery("Select afdeling, afdelingseq from bestelling where leverancier = '"+projekt[0]+"' and ordernummer = '"+projekt[1]+"'");
				System.out.println("Select afdeling, afdelingseq from bestelling where leverancier = '"+projekt[0]+"' and ordernummer = '"+projekt[1]+"'");
				while(rs7.next()){
					nadrzedny1 = rs7.getString("afdeling");
					nadrzedny2 = rs7.getString("afdelingseq");
					String status = "";
					
					//sprawdz czy otwarty!
					System.out.println("Select afdeling, afdelingseq, statuscode from bestelling where leverancier = '"+nadrzedny1+"' and ordernummer = '"+nadrzedny2+"'");
					Statement st8 = conn.createStatement();
					ResultSet rs8 = st8.executeQuery("Select statuscode from bestelling where leverancier = '"+nadrzedny1+"' and ordernummer = '"+nadrzedny2+"'");
					while(rs8.next()){
						status = rs8.getString("statuscode");
					}
					rs8.close();
					st8.close();
					if(status==null)
						status="";
					if(status.equals("H")||nadrzedny1.equals("7")){
						//sprawdz nazwe podrzednego
						Statement st9 = conn.createStatement();
						ResultSet rs9 = st9.executeQuery("Select artikelomschrijving from bestellingdetail where leverancier = '"+projekt[0]+"' and ordernummer = '"+projekt[1]+"'");
						while(rs9.next()){
							nazwa = rs9.getString("artikelomschrijving");
						}
						rs9.close();
						st9.close();
						Statement st5 = conn.createStatement();
						st5.executeUpdate("Update PartsOverview set Description = '"+nazwa+"' where OrderNo = '"+zamowienie+"'");
						System.out.println("Update PartsOverview set Description = '"+nazwa+"' where OrderNo = '"+zamowienie+"'");
						st5.close();
					}
				}
				rs7.close();
				st7.close();
				
			}
			rs6.close();
			st6.close();
			
			//naniesienie dobrych dat dla zleceñ serwisowych
			
			
			Statement st12 = conn.createStatement();
			ResultSet rs12 = st12.executeQuery("SELECT NrMaszyny from Calendar where Zakonczone = 0 and nrMaszyny like '14/%'");
			while(rs12.next()){
				String glownyProjekt = rs12.getString("NrMaszyny");
				//sprawdzenie czy podprojekt jest zamkniêty
				Statement st13 = conn.createStatement();
				String sql14 = "SELECT bestelling.leverdatum, bestellingdetail.leveringsdatumvoorzien from bestelling "
						+ "join bestellingdetail on bestelling.leverancier = bestellingdetail.leverancier and bestelling.ordernummer = bestellingdetail.ordernummer "
						+ "where bestelling.leverancierordernummer = '"+glownyProjekt+"'";
				ResultSet rs13 = st13.executeQuery(sql14);
				while(rs13.next()){
					String dataProdukcji = rs13.getString(2);
					String dataMontazu = rs13.getString(1);
					System.out.println(sql14+"  dataProdukcji = "+dataProdukcji+", dataMontazu = "+dataMontazu);
					Statement st5 = conn.createStatement();
					st5.executeUpdate("Update Calendar set dataProdukcji = '"+dataProdukcji+"', DataKoniecMontazu = '"+dataMontazu+"' where NrMaszyny = '"+glownyProjekt+"'");
					st5.close();
					
				}
				rs13.close();
				st13.close();
				
			}
			
			int p = 1;
			while(p>0) {
				p=0;
				//sprawdzenie czy GTT nie pominelo podprojektów
				Statement st14 = conn.createStatement();
				ResultSet rs14 = st14.executeQuery("select distinct OrderNo, bestelling.afdeling, bestelling.afdelingseq from partsoverview "
						+ "join bestelling on partsoverview.OrderNo = bestelling.leverancierordernummer "
						+ "where Description <> 'subproject' and afdeling <> 0 and afdeling <> 7 and orderno not like '14/%' ");
				while(rs14.next()) {
					String projNadrzedny = rs14.getString("afdeling")+"/"+rs14.getString("afdelingseq");
					String projZmieniany = rs14.getString("orderno");
					Statement st14a = conn.createStatement();
					ResultSet rs14a = st14a.executeQuery("Select statuscode from bestelling where leverancierordernummer = '"+projNadrzedny+"'");
					String status14a = "";
					while(rs14a.next()) {
						status14a = rs14a.getString("statuscode");
					}
					rs14a.close();st14a.close();
					if(!status14a.equals("H")) {
						String sql01 = "update partsoverview set orderno = '"+projNadrzedny+"', description = 'tymczasowy' where orderno = '"+projZmieniany+"'";
						System.out.println(sql01);
						Statement st15 = conn.createStatement();
						p+= st15.executeUpdate(sql01);
						st15.close();
					}
				}
				rs14.close(); st14.close();
				System.out.println(p);
			}
			Statement st16 = conn.createStatement();
			ResultSet rs16 = st16.executeQuery("Select distinct OrderNo, bestellingdetail.artikelomschrijving from partsoverview "
					+ "left join bestellingdetail on partsoverview.orderno = concat(bestellingdetail.leverancier, '/', bestellingdetail.ordernummer) "
					+ "where description = 'tymczasowy' and bestellingdetail.sequentie = 1");
			while(rs16.next()) {
				String orderno = rs16.getString("orderno");
				String desc = rs16.getString("artikelomschrijving");
				String sql01 = "update partsoverview set description = '"+desc+"' where orderno = '"+orderno+"' and description = 'tymczasowy' ";
				System.out.println(sql01);
				Statement st15 = conn.createStatement();
				st15.executeUpdate(sql01);
				st15.close();
			}
			rs16.close();st16.close();
			
			
			//SPRAWDZENIE PODPROJEKTÓW! 
			//sprawdzenie czy przypisane podprojekty s¹ jeszcze otwarte!
			Statement st10 = conn.createStatement();
			ResultSet rs10 = st10.executeQuery("SELECT NrMaszyny, Komentarz from Calendar where Zakonczone = 0 and (Komentarz like '2/%' or Komentarz like '6/%')");
			while(rs10.next()){
				String podprojekt = rs10.getString("Komentarz");
				String glownyProjekt = rs10.getString("NrMaszyny");
				//sprawdzenie czy podprojekt jest zamkniêty
				Statement st11 = conn.createStatement();
				ResultSet rs11 = st11.executeQuery("SELECT Zakonczone from Calendar where NrMaszyny ='"+podprojekt+"'");
				while(rs11.next()){
					String status = rs11.getString("Zakonczone");
					//jeœli projekt (np 6/...) jest ju¿ zamkniêty to usuwamy informacjê o zmianie numeru
					if(status.equals("1")){
						Statement st5 = conn.createStatement();
						st5.executeUpdate("Update Calendar set Komentarz = '' where NrMaszyny = '"+glownyProjekt+"'");
						st5.close();
					}
				}
				rs11.close();
				st11.close();
				
			}
			rs10.close();
			st10.close();	
			
			conn.close();
		}catch (Exception e)
		{
			e.printStackTrace();
		}
		System.out.println("end subprojects");
		return ;
	}

}
