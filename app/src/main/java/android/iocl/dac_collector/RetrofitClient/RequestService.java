package android.iocl.dac_collector.RetrofitClient;

import android.iocl.dac_collector.ModelData.BaseUpdateResponse;
import android.iocl.dac_collector.ModelData.search_consumer;
import android.iocl.dac_collector.ModelData.search_consumer_response;
import android.iocl.dac_collector.ModelData.update_consumer;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RequestService {
    public static final String KEY = "AKfycby562D7njxe7anUjn0R_LuUCo7z0IscpOl8z3dO-bA6xd8dcrm9LvmxxA0KK14Dm3q6";


    @POST("/macros/s/" + KEY + "/exec")
    Call<search_consumer_response> search_customer (@Body search_consumer body);

    @POST("/macros/s/" + KEY + "/exec")
    Call<BaseUpdateResponse> update_user (@Body update_consumer body);
}
