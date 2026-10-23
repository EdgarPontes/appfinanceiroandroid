package br.com.tecpontes.appfinanceiro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import br.com.tecpontes.appfinanceiro.model.StatementEntryDto;
import br.com.tecpontes.appfinanceiro.utils.StatementFileParser;
import java.util.List;
import org.junit.Test;

public class StatementFileParserTest {
    @Test public void parsesCsvWithSemicolonAndQuotedDescription() {
        List<StatementEntryDto> entries = StatementFileParser.parseCsv(
                "Data;Valor;Descrição;Reference_ID\n01/02/2026;-1.234,56;\"Mercado; Central\";fit-1");

        assertEquals(1, entries.size());
        assertEquals("DEBIT", entries.get(0).type);
        assertEquals("Mercado; Central", entries.get(0).description);
        assertEquals(1234.56, entries.get(0).amount, 0.001);
        assertTrue(entries.get(0).date.startsWith("2026-02-01"));
    }

    @Test public void parsesOfxDebitAndKeepsFitId() {
        String ofx = "<OFX><BANKTRANLIST><STMTTRN><TRNTYPE>DEBIT</TRNTYPE>"
                + "<DTPOSTED>20260304120000[-3:BRT]</DTPOSTED><TRNAMT>-25.90</TRNAMT>"
                + "<FITID>abc123</FITID><MEMO>Farmácia</MEMO></STMTTRN></BANKTRANLIST></OFX>";
        List<StatementEntryDto> entries = StatementFileParser.parseOfx(ofx);

        assertEquals(1, entries.size());
        assertEquals("abc123", entries.get(0).fitId);
        assertEquals("DEBIT", entries.get(0).type);
        assertEquals(25.90, entries.get(0).amount, 0.001);
        assertEquals("Farmácia", entries.get(0).description);
    }
}
