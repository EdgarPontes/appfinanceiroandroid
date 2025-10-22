package br.com.tecpontes.appfinanceiro.di;

import android.util.Log;
import br.com.tecpontes.appfinanceiro.network.ApiService;
import br.com.tecpontes.appfinanceiro.network.JwtInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import javax.inject.Singleton;

/**
 * Módulo Hilt para configuração de rede (Retrofit + OkHttp)
 */
@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    //private static final String BASE_URL = "https://appfinanceiro.tecpontes.com.br/"; // TODO: Configurar URL base
    private static final String BASE_URL = "http://10.0.2.2:5000/"; // Use 10.0.2.2 for emulator to connect to localhost
    private static final String TAG = "NetworkModule";

    @Provides
    @Singleton
    public Gson provideGson() {
        return new GsonBuilder()
                .setLenient()
                .create();
    }

    @Provides
    @Singleton
    public HttpLoggingInterceptor provideLoggingInterceptor() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        return logging;
    }

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(
            JwtInterceptor jwtInterceptor,
            HttpLoggingInterceptor loggingInterceptor
    ) {
        return new OkHttpClient.Builder()
                .addInterceptor(jwtInterceptor)
                .addInterceptor(loggingInterceptor)
                .build();
    }

    @Provides
    @Singleton
    public Retrofit provideRetrofit(Gson gson, OkHttpClient okHttpClient) {
        Log.d(TAG, "Configurando Retrofit com base URL: " + BASE_URL);

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    @Provides
    @Singleton
    public ApiService provideApiService(Retrofit retrofit) {
        return retrofit.create(ApiService.class);
    }
}
