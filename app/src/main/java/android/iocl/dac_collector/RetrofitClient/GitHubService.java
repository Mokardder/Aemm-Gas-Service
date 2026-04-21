package android.iocl.dac_collector.RetrofitClient;

import android.iocl.dac_collector.ModelData.GitHubRelease;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface GitHubService {
    // Get all releases
    @GET("repos/{owner}/{repo}/releases/latest")
    Call<GitHubRelease> getLatestRelease(@Path("owner") String owner,
                                         @Path("repo") String repo,
                                         @Header("Authorization") String authToken);
}