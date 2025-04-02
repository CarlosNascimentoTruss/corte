package br.com.sankhya.truss.simprod;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

public class ProcessaArquivo implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
		Registro[] linhas = ctx.getLinhas();
		JapeWrapper arqDAO = JapeFactory.dao("AD_UPLSIMPROD");
		
		for(Registro linha : linhas) {
			byte[] arquivo = arqDAO.findByPK((BigDecimal) linha.getCampo("NUUPL")).asBlob("ARQUIVO");
			
			Reader r = new InputStreamReader(new ByteArrayInputStream(arquivo));
			
			BufferedReader br = new BufferedReader(r);
			
			  StringBuilder clobData = new StringBuilder();
              String line;
              
              List<String> produtos = new ArrayList<>();
              List<String> quantidades = new ArrayList<>();
              
              int count = 0;
              while ((line = br.readLine()) != null) {
            	  if(count > 0) {
            		  
            		  int endIndex = line.indexOf(",");
            		  
	            	  String erro = "Produto: " + line.substring(0,endIndex) + "\n" +
	            	                "Quantidade: " + line.substring(endIndex + ",".length()).replace("\"", "");

	            	  produtos.add(line.substring(0,endIndex));
	            	  quantidades.add(formataNumero(line.substring(endIndex + ",".length()).replace("\"", "")));
	            	  
	                  clobData.append(line).append("\n");
            	  }
            	  count ++;
              }
              
              int endIndex = clobData.indexOf("__end_fileinformation__");
              
              BigDecimal nuupl = (BigDecimal) linha.getCampo("NUUPL");
              
              for(int i = 0 ; i< produtos.size(); i++) {
            	  new SimProd().simulaProducao(new BigDecimal(produtos.get(i)), new BigDecimal(quantidades.get(i)), nuupl);
              }
              linha.setCampo("DHIMP", TimeUtils.getNow());
		}
		
		ctx.setMensagemRetorno("Arquivo(s) Processado(s)");
	}
	
	public String formataNumero(String str)
	{
	        str = str.replace(".","");
	        str = str.replace(",", ".");
	        str = str.trim();

	        return str;
	}
	
}
