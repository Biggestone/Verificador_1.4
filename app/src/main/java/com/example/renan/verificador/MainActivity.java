package com.example.renan.verificador;

import com.microsoft.projectoxford.face.*;
import com.microsoft.projectoxford.face.contract.*;
import java.io.*;
import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import android.os.Process;
import android.support.v4.app.FragmentActivity;
import android.view.*;
import android.graphics.*;
import android.widget.*;
import android.provider.*;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.jar.Manifest;

public class MainActivity extends AppCompatActivity {
    private ImageView imagem1;
    private ImageView imagem2;
    private File mCurrentPhotoPath;
    private Bitmap bitmap;
    private TextView textView;
    private Uri mUriPhotoTaken;
    private TextView txtpath;
    final private int MY_RESQUEST_CAMERA =4;
    static final int REQUEST_IMAGE_CAPTURE = 2;
    static final int REQUEST_WRITE = 3;
    private String MY_KEY_FOR_PHOTOS="123";
    final private int MY_CODE_OPEN_FOLDER =100;
    private int idDeViews;
    private Bitmap mBitmap0;
    private Bitmap mBitmap1;
    private UUID mFaceId0;
    private UUID mFaceId1;
    private int mIndex;//Index global que será enviado para identificação das faces
    private String myLocalFilePath;
    private final int PICK_IMAGE = 1;
    private ProgressDialog detectionProgressDialog;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imagem1 = (ImageView)findViewById(R.id.imagem1);
        imagem2 = (ImageView)findViewById(R.id.imagem2);
        txtpath = (TextView)findViewById(R.id.pathImagem);
        idDeViews = 0;
        detectionProgressDialog = new ProgressDialog(this);
        progressDialog = new ProgressDialog(this);
    }


    private class VerificationTask extends AsyncTask<Void, String, VerifyResult> {
        // Id das duas faces a serem verificadas
        private UUID mFaceId0;
        private UUID mFaceId1;

        VerificationTask (UUID faceId0, UUID faceId1) {
            mFaceId0 = faceId0;
            mFaceId1 = faceId1;
        }

        @Override
        protected VerifyResult doInBackground(Void... params) {

            // Pega uma instância do face service para o início da verificação
            FaceServiceClient faceServiceClient = new FaceServiceRestClient("635239d49d1f4f1dbf068ae4fa72949d");

            try{
                publishProgress("Verificando...");

                // Inicia a verificação.
                return faceServiceClient.verify(
                        mFaceId0,      /* The first face ID to verify */
                        mFaceId1);     /* The second face ID to verify */
            }  catch (Exception e) {
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            progressDialog.show();
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            progressDialog.setMessage(progress[0]);
            setInfo(progress[0]);
        }

        @Override
        protected void onPostExecute(VerifyResult result) {
            // Mosta o resultado da verificação
            setUiAfterVerification(result);
        }
    }

    private void setUiAfterVerification(VerifyResult result) {
        // Quando a verificação acaba, o progress dialog é finalizado.
        progressDialog.dismiss();

        // Mostra o resultado da verificação.
        if (result != null) {
            DecimalFormat formatter = new DecimalFormat("#0.00");
            String verificationResult = (result.isIdentical ? "Pessoas iguais": "Pessoas diferentes")
                    + ". A confidência é " + formatter.format(result.confidence);
            setInfo(verificationResult);
        }else{
            String connectionFailed = "A conexão com o serviço falhou. Tente novamente";
            setInfo(connectionFailed);
        }
    }

    private void setInfo(String info) {
        TextView textView = (TextView) findViewById(R.id.result);
        textView.setText(info);
    }

    // Detecta faces quando feito o upload de uma imagem
    // Enquadra faces depois de reconhecê-las

    private void detectAndFrame(final Bitmap imageBitmap, final int index)
    {
        final FaceServiceClient faceServiceClient =
                new FaceServiceRestClient("635239d49d1f4f1dbf068ae4fa72949d");
        mIndex= index;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());
        AsyncTask<InputStream, String, Face[]> detectTask =
                new AsyncTask<InputStream, String, Face[]>() {
                    @Override
                    protected Face[] doInBackground(InputStream... params) {
                        try {
                            publishProgress("Detectando faces...");
                            Face[] result = faceServiceClient.detect(
                                    params[0],
                                    true,         // returnFaceId
                                    false,        // returnFaceLandmarks
                                    null           // returnFaceAttributes: a string like "age, gender"
                            );
                            if (result == null)
                            {
                                publishProgress("Detecção finalizada. Nada foi encontrado");
                                return null;
                            }
                            publishProgress(
                                    String.format("Processo finalizado. %d face(s) detectada",
                                            result.length));
                            return result;
                        } catch (Exception e) {
                            publishProgress("Ocorreu um erro durante o processo de identificação");

                            return null;
                        }
                    }
                    @Override
                    protected void onPreExecute() {
                        progressDialog.show();
                    }
                    @Override
                    protected void onProgressUpdate(String... progress) {
                        progressDialog.setMessage(progress[0]);
                    }
                    @Override
                    protected void onPostExecute(Face[] result) {

                        if(mIndex==0){
                            if(!(result==null)){
                                FacelistAdapter facelistAdapter0 = new FacelistAdapter(result,mIndex);
                                int position = facelistAdapter0.getCount()-1;
                                mFaceId0 = facelistAdapter0.faces.get(position).faceId;
                            }
                        }
                        else if(mIndex==1){
                            FacelistAdapter facelistAdapter1 = new FacelistAdapter(result,mIndex);
                            int position = facelistAdapter1.getCount()-1;
                            mFaceId1 = facelistAdapter1.faces.get(position).faceId;
                        }
                    }
                };
        detectTask.execute(inputStream);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
            {
                // Se o request for cancelado, o array fica vazio
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    iniciarCamera();

                } else {

                    return;
                }

            }

        }
    }

    //Métodos que são chamados quando os botões são clicados
    //1- Botão tirar foto
    public void tirarFoto(View view){
        idDeViews = view.getId();
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
            pedirAcesso();
        }else{
            iniciarCamera();
        }
    }

    //2-Botão abrir diretório
    public void abrirDiretorio(View view){
        idDeViews = view.getId();
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent,"Selecione um file manager"),MY_CODE_OPEN_FOLDER);
    }

    //Pede acesso se ele ainda não foi aceito
    private void pedirAcesso() {

        ActivityCompat.requestPermissions(this, new String[]{
                android.Manifest.permission.CAMERA},REQUEST_IMAGE_CAPTURE);
    }

    public void iniciarCamera(){
        //metodo desativado temporariamente para continuação amanhã
        /*Intent cameraIntent = new Intent(this,CameraActivity.class);
        startActivity(cameraIntent);*/
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(cameraIntent.resolveActivity(getPackageManager())!=null){
            myLocalFilePath = getExternalFilesDir(null)+"/"+System.currentTimeMillis()+".png";
            File file = new File(myLocalFilePath);
            mUriPhotoTaken = Uri.fromFile(file);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mUriPhotoTaken);
            startActivityForResult(cameraIntent, MY_RESQUEST_CAMERA);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri uriImagem = null;
        if(requestCode==MY_CODE_OPEN_FOLDER && resultCode == RESULT_OK){
            uriImagem =data.getData();
            try {
                InputStream is = getContentResolver().openInputStream(uriImagem);
                bitmap = BitmapFactory.decodeStream(is);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            if(idDeViews==R.id.btEscolherFoto1){
                imagem1.setImageBitmap(bitmap);
                mBitmap0 = bitmap;
                detectAndFrame(bitmap,0);
            }else if(idDeViews==R.id.btEscolherFoto2){
                imagem2.setImageBitmap(bitmap);
                mBitmap1 = bitmap;
                detectAndFrame(bitmap,1);
            }
        }

        try {
            if(requestCode==MY_RESQUEST_CAMERA && resultCode == RESULT_OK){
                Bitmap imagemFoto = BitmapFactory.decodeFile(myLocalFilePath);
                Bitmap imagemFotoReduzida = Bitmap.createScaledBitmap(imagemFoto,144,256,true);
                if(idDeViews==R.id.btTirarFoto1){
                    imagem1.setImageBitmap(imagemFotoReduzida);
                    mBitmap0 = imagemFoto;
                    detectAndFrame(imagemFoto,0);
                    idDeViews = 0;
                }
                else if(idDeViews==R.id.bttirarFoto2){
                    imagem2.setImageBitmap(imagemFotoReduzida);
                    mBitmap1 = imagemFoto;
                    detectAndFrame(imagemFoto,1);
                    idDeViews = 0;
                }
            }
        }catch (Exception e){
            Toast.makeText(getBaseContext(),"OOps, algo deu errado",Toast.LENGTH_LONG).show();
            String log = e.toString();
            Intent intent = new Intent(this,LogActiviyActivity.class);
            intent.putExtra("log",log);
            startActivity(intent);
        }


    }

    public void verify(View view) {
        new VerificationTask(mFaceId0, mFaceId1).execute();

    }

    private class FacelistAdapter extends BaseAdapter{

        List<Face> faces;
        List<Bitmap> bitmapFaces;
        int mIndexFacelist;
        FacelistAdapter (Face[] detectionResult, int index){
            faces = new ArrayList<>();
            mIndexFacelist = index;

            if(detectionResult != null){
                faces = Arrays.asList(detectionResult);
            }
        }



        @Override
        public int getCount() {
            return faces.size();
        }

        @Override
        public Object getItem(int position) {
            return faces.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }
    }


}
