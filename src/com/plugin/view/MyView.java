package com.plugin.view;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.IAxisSet;
import org.eclipse.swtchart.IBarSeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.ui.part.ViewPart;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MyView extends ViewPart {
    public static final String ID = "com.plugin.view.view";

    // --- UI Components & State Management ---
    private Composite chartComposite;
    private Table table;
    private Button confirmButton;

    // Armazena os caminhos dos arquivos: [0] = smelldsl, [1] = csv/json 1, [2] = csv/json 2
    private final String[] filePaths = new String[3]; 
    private Text[] fileNameTexts = new Text[3];
    private Canvas[] statusIndicators = new Canvas[3];
    
    private final String[] metricas = {"LOC", "NOA", "NOM", "WMC", "LCOM", "CBO", "DIT", "NOC"};

    // Cores personalizadas para uma aparência mais moderna
    private Color greenColor;
    private Color redColor;
    private Color primaryBlue;
    private Color lightGrayBackground;
    private Color fontColor;

    @Override
    public void createPartControl(Composite parent) {
        Display display = parent.getDisplay();
        greenColor = new Color(display, 76, 175, 80);
        redColor = new Color(display, 244, 67, 54);
        primaryBlue = new Color(display, 33, 150, 243);
        lightGrayBackground = new Color(display, 245, 245, 245);
        fontColor = display.getSystemColor(SWT.COLOR_DARK_GRAY);

        parent.setLayout(new GridLayout(1, false));
        parent.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

        createFileManagementArea(parent);
        criarTabelaMetricas(parent);
        criarAreaGrafico(parent);
    }
    
    private void createFileManagementArea(Composite parent) {
        Group fileGroup = new Group(parent, SWT.NONE);
        fileGroup.setText("Gerenciamento de Arquivos");
        fileGroup.setLayout(new GridLayout(5, false));
        fileGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        fileGroup.setFont(new Font(parent.getDisplay(), "Segoe UI", 9, SWT.BOLD));
        
        GridLayout groupLayout = (GridLayout) fileGroup.getLayout();
        groupLayout.marginWidth = 10;
        groupLayout.marginHeight = 10;
        groupLayout.verticalSpacing = 8;

        createFileSlot(fileGroup, 0, "Arquivo DSL (*.smelldsl)", new String[]{"*.smelldsl"});
        createFileSlot(fileGroup, 1, "Métricas 1 (*.csv, *.json)", new String[]{"*.csv", "*.json"});
        createFileSlot(fileGroup, 2, "Métricas 2 (*.csv, *.json)", new String[]{"*.csv", "*.json"});

        confirmButton = new Button(fileGroup, SWT.PUSH);
        confirmButton.setText("Confirmar e Processar");
        GridData confirmButtonData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
        confirmButtonData.horizontalSpan = 5;
        confirmButtonData.verticalIndent = 10;
        confirmButton.setLayoutData(confirmButtonData);
        confirmButton.setEnabled(false);
        confirmButton.setFont(new Font(parent.getDisplay(), "Segoe UI", 9, SWT.NORMAL));

        confirmButton.addListener(SWT.Selection, e -> processarArquivosConfirmados());
    }

    private void createFileSlot(Composite parent, final int index, String labelText, String[] extensions) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(labelText);
        label.setFont(new Font(parent.getDisplay(), "Segoe UI", 9, SWT.NORMAL));

        fileNameTexts[index] = new Text(parent, SWT.BORDER | SWT.READ_ONLY);
        fileNameTexts[index].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        fileNameTexts[index].setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        fileNameTexts[index].setFont(new Font(parent.getDisplay(), "Consolas", 9, SWT.NORMAL));

        statusIndicators[index] = new Canvas(parent, SWT.NONE);
        GridData canvasData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        canvasData.widthHint = 16;
        canvasData.heightHint = 16;
        statusIndicators[index].setLayoutData(canvasData);
        statusIndicators[index].addPaintListener(e -> {
            Color color = filePaths[index] != null ? greenColor : redColor;
            e.gc.setBackground(color);
            e.gc.fillOval(0, 0, 15, 15);
        });

        Button attachButton = new Button(parent, SWT.PUSH);
        attachButton.setText("Anexar...");
        attachButton.setFont(new Font(parent.getDisplay(), "Segoe UI", 9, SWT.NORMAL));
        attachButton.addListener(SWT.Selection, e -> {
            FileDialog dialog = new FileDialog(getSite().getShell(), SWT.OPEN);
            dialog.setFilterExtensions(extensions);
            String path = dialog.open();
            if (path != null) {
                filePaths[index] = path;
                fileNameTexts[index].setText(new File(path).getName());
                statusIndicators[index].redraw();
                checkConfirmButtonState();
            }
        });

        Button deleteButton = new Button(parent, SWT.PUSH);
        deleteButton.setText("Excluir");
        deleteButton.setFont(new Font(parent.getDisplay(), "Segoe UI", 9, SWT.NORMAL));
        deleteButton.addListener(SWT.Selection, e -> {
            filePaths[index] = null;
            fileNameTexts[index].setText("");
            statusIndicators[index].redraw();
            checkConfirmButtonState();
        });
    }

    private void checkConfirmButtonState() {
        boolean allFilesLoaded = Arrays.stream(filePaths).allMatch(p -> p != null);
        confirmButton.setEnabled(allFilesLoaded);
    }

    private void processarArquivosConfirmados() {
        if (!confirmButton.isEnabled()) {
            MessageDialog.openError(getSite().getShell(), "Erro", "Nem todos os arquivos foram carregados.");
            return;
        }
        
        String nomesDosArquivos = Arrays.stream(filePaths)
                .map(p -> new File(p).getName())
                .collect(Collectors.joining("\n- ", "\n- ", ""));

        MessageDialog.openInformation(
                getSite().getShell(),
                "Sucesso",
                "Arquivos confirmados com sucesso:" + nomesDosArquivos
        );
        
        String primeiroCsv = filePaths[1];
        carregarDadosDeUmCSV(primeiroCsv);
    }
    
    private void criarTabelaMetricas(Composite parent) {
        table = new Table(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        table.setFont(new Font(parent.getDisplay(), "Segoe UI", 9, SWT.NORMAL));
        table.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

        String[] colunas = {"Métrica", "Valor", "Classificação"};
        for (String col : colunas) {
            TableColumn column = new TableColumn(table, SWT.LEFT);
            column.setText(col);
        }
    }

    private void criarAreaGrafico(Composite parent) {
        chartComposite = new Composite(parent, SWT.NONE);
        chartComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        chartComposite.setLayout(new FillLayout());
    }
    
    private void carregarDadosDeUmCSV(String caminhoArquivo) {
        limparTabelaEGrafico();
        try (BufferedReader br = new BufferedReader(new FileReader(caminhoArquivo))) {
            String linha = br.readLine();
            if (linha == null) throw new IOException("Arquivo vazio");

            String[] valores = linha.split(",");
            if (valores.length != metricas.length)
                throw new IOException("Esperado " + metricas.length + " valores de métricas");

            double[] valoresY = processarMetricas(valores);
            gerarGrafico(valoresY);

        } catch (Exception e) {
            MessageDialog.openError(getSite().getShell(), "Erro", "Falha ao processar CSV: " + e.getMessage());
        }
    }

    private void limparTabelaEGrafico() {
        table.removeAll();
        for (Control child : chartComposite.getChildren()) child.dispose();
        if(chartComposite != null && !chartComposite.isDisposed()){
            chartComposite.layout();
        }
    }

    private double[] processarMetricas(String[] valores) {
        double[] valoresNumericos = new double[metricas.length];
        for (int i = 0; i < valores.length; i++) {
            String valorStr = valores[i].trim();
            double valor = Double.parseDouble(valorStr);
            String classificacao = classificar(metricas[i], valor);
            adicionarLinhaTabela(metricas[i], valorStr, classificacao);
            valoresNumericos[i] = switch (classificacao) {
                case "LOW" -> 1.0;
                case "MEDIUM" -> 2.0;
                case "HIGH" -> 3.0;
                default -> 0.0;
            };
        }
        for (TableColumn col : table.getColumns()) col.pack();
        return valoresNumericos;
    }

    private void adicionarLinhaTabela(String metrica, String valor, String classificacao) {
        TableItem item = new TableItem(table, SWT.NONE);
        item.setText(new String[]{metrica, valor, classificacao});
        item.setForeground(fontColor);
    }

    @SuppressWarnings("deprecation")
	private void gerarGrafico(double[] valoresNumericos) {
        Display display = chartComposite.getDisplay();
        Chart chart = new Chart(chartComposite, SWT.NONE);
        
        // --- Estilização Moderna do Gráfico ---
        chart.setBackground(lightGrayBackground);
        chart.getTitle().setText("Classificação das Métricas");
        chart.getTitle().setFont(new Font(display, "Segoe UI", 11, SWT.BOLD));
        chart.getTitle().setForeground(fontColor);

        IAxisSet axisSet = chart.getAxisSet();
        
        // --- EIXO Y ---
        IAxis yAxis = axisSet.getYAxis(0);
        // CORREÇÃO: Usar getTitle() em vez de getAxisLabel()
        yAxis.getTitle().setText("Classificação (1=Low, 2=Medium, 3=High)"); 
        yAxis.getTitle().setFont(new Font(display, "Segoe UI", 9, SWT.NORMAL));
        yAxis.getTitle().setForeground(fontColor);
        yAxis.getTick().setFont(new Font(display, "Segoe UI", 8, SWT.NORMAL));
        yAxis.getTick().setForeground(fontColor);
        yAxis.getGrid().setForeground(new Color(display, 224, 224, 224)); // Linhas de grade suaves
        
        // --- EIXO X ---
        IAxis xAxis = axisSet.getXAxis(0);
        // CORREÇÃO: Usar getTitle() em vez de getAxisLabel()
        xAxis.getTitle().setText("Métricas");
        xAxis.getTitle().setFont(new Font(display, "Segoe UI", 9, SWT.NORMAL));
        xAxis.getTitle().setForeground(fontColor);
        xAxis.getTick().setFont(new Font(display, "Segoe UI", 8, SWT.NORMAL));
        xAxis.getTick().setForeground(fontColor);
        xAxis.setCategorySeries(metricas);
        xAxis.getGrid().setVisible(false); // Remove as linhas de grade verticais para um visual mais limpo

        double[] xSeries = IntStream.range(0, metricas.length).mapToDouble(i -> (double) i).toArray();
        IBarSeries series = (IBarSeries) chart.getSeriesSet().createSeries(SeriesType.BAR, "Classificação");
        series.setXSeries(xSeries);
        series.setYSeries(valoresNumericos);
        series.setBarColor(primaryBlue);

        chart.getLegend().setPosition(SWT.BOTTOM);
        chart.getLegend().setFont(new Font(display, "Segoe UI", 8, SWT.NORMAL));
        chart.getLegend().setForeground(fontColor);

        axisSet.adjustRange();
        chartComposite.layout();
    }

    private String classificar(String metrica, double valor) {
        return switch (metrica) {
            case "LOC" -> valor < 100 ? "LOW" : valor < 300 ? "MEDIUM" : "HIGH";
            case "NOA" -> valor < 5 ? "LOW" : valor < 10 ? "MEDIUM" : "HIGH";
            case "NOM" -> valor < 5 ? "LOW" : valor < 15 ? "MEDIUM" : "HIGH";
            case "WMC" -> valor < 10 ? "LOW" : valor < 30 ? "MEDIUM" : "HIGH";
            case "LCOM" -> valor < 0.3 ? "LOW" : valor < 0.7 ? "MEDIUM" : "HIGH";
            case "CBO" -> valor < 5 ? "LOW" : valor < 15 ? "MEDIUM" : "HIGH";
            case "DIT" -> valor < 2 ? "LOW" : valor < 5 ? "MEDIUM" : "HIGH";
            case "NOC" -> valor == 0 ? "LOW" : valor < 5 ? "MEDIUM" : "HIGH";
            default -> "UNKNOWN";
        };
    }

    @Override
    public void setFocus() {
        if(table != null && !table.isDisposed()) {
            table.setFocus();
        }
    }

    @Override
    public void dispose() {
        if (greenColor != null) greenColor.dispose();
        if (redColor != null) redColor.dispose();
        if (primaryBlue != null) primaryBlue.dispose();
        if (lightGrayBackground != null) lightGrayBackground.dispose();
        // fontColor é uma cor do sistema, não precisa de dispose.
        super.dispose();
    }
}