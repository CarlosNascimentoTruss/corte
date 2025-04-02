package br.com.sankhya.truss.impverba.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/* 
 * Modelo utilizado na Rotina que realiza a importação de um csv com um determinado template para a tela controle de verbas.
 * Código para atendimento do seguinte card: https://grupoboticario.kanbanize.com/ctrl_board/301/cards/1578963/details/
 * 
 * */

public class ImpVerbaModel {
	
	Timestamp dtInclusao;
	BigDecimal vlrInicialVerba;
	BigDecimal codemp;
	BigDecimal codnat;
	Timestamp dtInicial;
	Timestamp dtFinal;
	BigDecimal idImp;
	String cnpjParc;
	
	public Timestamp getDtInclusao() {
		return dtInclusao;
	}
	public void setDtInclusao(Timestamp dtInclusao) {
		this.dtInclusao = dtInclusao;
	}
	public BigDecimal getVlrInicialVerba() {
		return vlrInicialVerba;
	}
	public void setVlrInicialVerba(BigDecimal vlrInicialVerba) {
		this.vlrInicialVerba = vlrInicialVerba;
	}
	public BigDecimal getCodemp() {
		return codemp;
	}
	public void setCodemp(BigDecimal codemp) {
		this.codemp = codemp;
	}
	public BigDecimal getCodnat() {
		return codnat;
	}
	public void setCodnat(BigDecimal codnat) {
		this.codnat = codnat;
	}
	public Timestamp getDtInicial() {
		return dtInicial;
	}
	public void setDtInicial(Timestamp dtInicial) {
		this.dtInicial = dtInicial;
	}
	public Timestamp getDtFinal() {
		return dtFinal;
	}
	public void setDtFinal(Timestamp dtFinal) {
		this.dtFinal = dtFinal;
	}
	public BigDecimal getIdImp() {
		return idImp;
	}
	public void setIdImp(BigDecimal idImp) {
		this.idImp = idImp;
	}
	public String getCnpjParc() {
		return cnpjParc;
	}
	public void setCnpjParc(String cnpjParc) {
		this.cnpjParc = cnpjParc;
	}
	
	
}
