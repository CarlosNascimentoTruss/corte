package br.com.sankhya.truss.impverba.action;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.Date;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.truss.impverba.helper.ImpVerbaHelper;
import br.com.sankhya.truss.impverba.model.ImpVerbaModel;

/* 
 * Rotina que realiza a importação de um csv com um determinado template para a tela controle de verbas.
 * Código para atendimento do seguinte card: https://grupoboticario.kanbanize.com/ctrl_board/301/cards/1578963/details/
 * 
 * */

public class ProcessaArquivo implements AcaoRotinaJava {
	
	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
		try {
			Registro[] linhas = ctx.getLinhas();
			// Faz o loop para as linhas selecionadas
			for(Registro linha : linhas) {
				Collection<ImpVerbaModel> verbasModel = new ArrayList<>();
				
				// Obtém metadados do arquivo
				byte[] arquivo = (byte[]) linha.getCampo("ARQUIVO");
				
				BigDecimal nuImportacao = (BigDecimal) linha.getCampo("NUIMPORTACAO");
				BigDecimal codusuImportacao = (BigDecimal) linha.getCampo("CODUSUIMPORTACAO");
				
				// Verifica se já teve planilha importada
				if(codusuImportacao != null) {
					ctx.mostraErro("Este registro já teve a planilha importada.");
				}
				
				Reader r = new InputStreamReader(new ByteArrayInputStream(arquivo));
				
				BufferedReader br = new BufferedReader(r);
	              String line;
	              
	              // Padrão para considerar números com separação de vírgula por decimais
	              int count = 0;
	              Pattern pattern = Pattern.compile("\"([^\"]*)\"|([^,]+)");
	              while ((line = br.readLine()) != null) {
	            	  if(count > 0) {
		            	  
		            	  ImpVerbaModel verbaModel = new ImpVerbaModel();
	            		  Matcher matcher = pattern.matcher(line);
	            		  List<String> valores = new ArrayList<>();
	            		  
	            		  while(matcher.find()) {
	            			  if (matcher.group(1) != null) {
	            	                valores.add(matcher.group(1));
	            	            } else {
	            	                valores.add(matcher.group(2)); 
	            	            }
	            		  }
	            		  int countLinha = 0;
	            		  // escreve cada informação da verba nas linhas
	            		  verbaModel.setIdImp(nuImportacao);
	            		  for(String valor : valores) {
	            			  countLinha++;
	            			  if(countLinha == 1) {
	            				  verbaModel.setDtInclusao(stringParaTimestamp(valor));
	            			  } else if(countLinha == 2) {
	            				  verbaModel.setVlrInicialVerba(new BigDecimal(formataNumero(valor)));
	            			  }	else if(countLinha == 3) {
	            				  verbaModel.setCodemp(new BigDecimal(valor));
	            			  } else if(countLinha == 4) {
	            				  verbaModel.setCodnat(new BigDecimal(valor));
	            			  } else if(countLinha == 5) {
	            				  verbaModel.setDtInicial(stringParaTimestamp(valor));
	            			  } else if(countLinha == 6) {
	            				  verbaModel.setDtFinal(stringParaTimestamp(valor));
	            			  } else if(countLinha == 7) {
	            				  verbaModel.setCnpjParc(valor);
	            			  }
	            		  }
	            		  
	            		  verbasModel.add(verbaModel);
	            	  }
	            	  count ++;
	              }
	              
	              // Chamada de método para dar insert na tabela de controle de verbas
	              new ImpVerbaHelper().insereVerbas(verbasModel, ctx.getUsuarioLogado(), nuImportacao);
	              linha.setCampo("DHIMPORTACAO", TimeUtils.getNow());
	              linha.setCampo("CODUSUIMPORTACAO", ctx.getUsuarioLogado());
	              
			}
			
			ctx.setMensagemRetorno("Arquivo(s) Processado(s)");
		} catch(Exception e) {
			e.printStackTrace();
			ctx.mostraErro("Erro ao processar o arquivo. " + e.getMessage());
		}
		
		
	}
	
	public static Timestamp stringParaTimestamp(String dataString) throws Exception {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            Date parsedDate = dateFormat.parse(dataString);
            return new Timestamp(parsedDate.getTime());
        } catch (Exception e) {
            throw new Exception("Erro ao converter data. Não foi possível converter a data " + dataString + ". A data deve ser informada no formato DD/MM/YYYY");
        }
    }
	
	public String formataNumero(String str)
	{
	        str = str.replace(".","");
	        str = str.replace(",", ".");
	        str = str.trim();

	        return str;
	}
	
}
