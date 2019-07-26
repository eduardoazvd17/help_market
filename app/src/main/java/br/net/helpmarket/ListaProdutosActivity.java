package br.net.helpmarket;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import br.net.helpmarket.adapter.ListaComprasAdapter;
import br.net.helpmarket.adapter.ListaProdutosAdapter;
import br.net.helpmarket.database.DBController;
import br.net.helpmarket.modelo.Compra;
import br.net.helpmarket.modelo.Lista;
import br.net.helpmarket.modelo.Produto;
import br.net.helpmarket.modelo.Usuario;

public class ListaProdutosActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private TextView nomeLista, gastoMaximo, totalGasto, totalEconomizado;
    private Lista lista;
    private List<Compra> compras;
    private ListView lvProdutos;
    private Produto produto;
    private ListaProdutosAdapter lpAdapter;
    private int numeroToken=1;
    private Tokens token = Tokens.TK1;
    private CoordinatorLayout layout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listaprodutos);

        toolbar = findViewById(R.id.lp_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        this.lista = (Lista) getIntent().getExtras().getSerializable("lista");
        nomeLista = findViewById(R.id.lp_nomeLista);
        nomeLista.setText(lista.getNome());
        lvProdutos = findViewById(R.id.lvProdutos);
        gastoMaximo = findViewById(R.id.lp_gastoMaximo);
        totalGasto = findViewById(R.id.lp_totalGasto);
        totalEconomizado = findViewById(R.id.lp_totalEconomizado);
        layout = findViewById(R.id.lp_layout);
        registerForContextMenu(lvProdutos);

        listarProdutos();
        atualizarFundo();
        calcularGastos();

        FloatingActionButton adicionarProduto = findViewById(R.id.adicionarProduto);
        adicionarProduto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator scanIntegrator = new IntentIntegrator(ListaProdutosActivity.this);
                scanIntegrator.setPrompt("Alinhe o código de barras com a linha para ser lido")
                        .setBeepEnabled(true)
                        .setOrientationLocked(true)
                        .setBarcodeImageEnabled(true)
                        .initiateScan();
            }
        });

        ImageButton voltar = findViewById(R.id.lp_voltar);
        voltar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.menu_longpress, menu);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Compra compraSelecionada = (Compra) lpAdapter.getItem(info.position);
        menu.setHeaderTitle(compraSelecionada.getProduto().getNome());
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final Compra compraSelecionada = (Compra) lpAdapter.getItem(info.position);
        switch (item.getItemId()) {
            case R.id.lpr_editar:
                Intent intent = new Intent(getBaseContext(), EditarProdutoActivity.class);
                intent.putExtra("compra", compraSelecionada);
                startActivity(intent);
                return true;
            case R.id.lpr_excluir:
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                DBController db = new DBController(getBaseContext());
                                db.deletarCompra(compraSelecionada.getId());
                                listarProdutos();
                                atualizarFundo();
                                calcularGastos();
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                Toast.makeText(getApplicationContext(), "Operação cancelada.", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                };
                AlertDialog.Builder ab = new AlertDialog.Builder(this);
                ab.setMessage("Deseja realmente excluir o produto " + compraSelecionada.getProduto().getNome() + "?")
                        .setNegativeButton("Não", dialogClickListener)
                        .setPositiveButton("Sim", dialogClickListener)
                        .show();
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (null == scanningResult) {
            Toast.makeText(this, "Não foi possivel efetuar a leitura deste codigo de barras", Toast.LENGTH_LONG).show();
        } else {
            try {
                buscarProduto(scanningResult.getContents());
                Intent i = new Intent(getBaseContext(), AdicionarProdutoActivity.class);
                i.putExtra("lista", lista);
                i.putExtra("produto", produto);
                startActivity(i);
            } catch (Exception e) {
                Toast.makeText(this, "Produto não encontrado...", Toast.LENGTH_LONG).show();
                gerarToken(numeroToken);
                numeroToken++;
            }
        }
    }

    private void gerarToken(int numeroToken) {
        if (numeroToken == 1) {
            this.token = Tokens.TK2;
        } else if (numeroToken == 2) {
            this.token = Tokens.TK3;
        } else if (numeroToken == 3) {
            this.token = Tokens.TK4;
        }
    }

    private void buscarProduto(String codigo) throws Exception {
        DBController db = new DBController(getBaseContext());
        Produto produtoDB = db.buscarProduto(Long.parseLong(codigo));
        if (null == produtoDB) {
            String token = this.token.getKey();
            String endpoint = "https://api.cosmos.bluesoft.com.br/";
            URL url = new URL(endpoint + "gtins/" + codigo);
            HttpURLConnection conexao = (HttpURLConnection) url.openConnection();
            conexao.setRequestMethod("GET");
            conexao.setRequestProperty("X-Cosmos-Token", token);
            conexao.connect();
            BufferedReader br = new BufferedReader(new InputStreamReader(conexao.getInputStream()));
            String jsonString = br.readLine();
            JSONObject jsonObject = new JSONObject(jsonString);
            this.produto = new Produto(Long.parseLong(jsonObject.getString("gtin")), jsonObject.getString("description"), jsonObject.getString("thumbnail"));
            this.produto.verificarPreenchimento();
            db.inserirProduto(produto);
            conexao.disconnect();
        } else {
            this.produto = produtoDB;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        listarProdutos();
        atualizarFundo();
        calcularGastos();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_lista_produtos, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.add_produto:
                IntentIntegrator scanIntegrator = new IntentIntegrator(ListaProdutosActivity.this);
                scanIntegrator.setPrompt("Alinhe o código de barras com a linha para ser lido")
                        .setBeepEnabled(true)
                        .setOrientationLocked(true)
                        .setBarcodeImageEnabled(true)
                        .initiateScan();
                break;
            case R.id.excluir_produtos:
                //TODO: Entrar em modo de seleção para deletar.
                break;
        }
        return super.onOptionsItemSelected(item);

    }

    private void calcularGastos() {
        gastoMaximo.setText(NumberFormat.getCurrencyInstance().format(lista.getGastoMaximo()));
        double total = 0;
        for (Compra c : compras) {
            total = total + ((c.getQuantidade()+0.0) * c.getPreco());
        }
        totalGasto.setText(NumberFormat.getCurrencyInstance().format(total));
        double economizado = lista.getGastoMaximo() - total;
        totalEconomizado.setText(NumberFormat.getCurrencyInstance().format(economizado));

        if (total > lista.getGastoMaximo()) {
            totalEconomizado.setTextColor(Color.RED);
            totalEconomizado.setError("As compras ultrapassaram o limite definido");
        }
    }

    public void listarProdutos() {
        DBController db = new DBController(getBaseContext());
        this.compras = db.selecionarCompras(lista);
        this.lpAdapter = new ListaProdutosAdapter(this.compras, this);
        this.lvProdutos.setAdapter(this.lpAdapter);
    }

    private void atualizarFundo() {
        if (compras.size() == 0) {
            layout.setBackground(getDrawable(R.drawable.lp_vazio));
        } else {
            layout.setBackground(getDrawable(R.color.colorWhite));
        }
    }
}
