/*
 *    Copyright 2017 ThirtyDegreesRay
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.thirtydegreesray.openhub.mvp.presenter;

import android.support.annotation.NonNull;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.dao.DaoSession;
import com.thirtydegreesray.openhub.http.core.HttpObserver;
import com.thirtydegreesray.openhub.http.core.HttpResponse;
import com.thirtydegreesray.openhub.http.error.HttpPageNoFoundError;
import com.thirtydegreesray.openhub.mvp.contract.ICommitsContract;
import com.thirtydegreesray.openhub.mvp.model.RepoCommit;
import com.thirtydegreesray.openhub.mvp.presenter.base.BasePagerPresenter;
import com.thirtydegreesray.openhub.util.StringUtils;

import java.util.ArrayList;

import javax.inject.Inject;

import retrofit2.Response;
import rx.Observable;

/**
 * Created by ThirtyDegreesRay on 2017/10/17 14:29:17
 */

public class CommitsPresenter extends BasePagerPresenter<ICommitsContract.View>
        implements ICommitsContract.Presenter {

    @AutoAccess String user ;
    @AutoAccess String repo ;
    @AutoAccess String branch ;
    private ArrayList<RepoCommit> commits;

    @Inject
    public CommitsPresenter(DaoSession daoSession) {
        super(daoSession);
    }

    @Override
    protected void loadData() {
        loadCommits(branch, false, 1);
    }

    @Override
    public void loadCommits(@NonNull final String branch, final boolean isReload, final int page) {
        this.branch = branch;
        mView.showLoading();
        final boolean readCacheFirst = !isReload && page == 1;
        HttpObserver<ArrayList<RepoCommit>> httpObserver = new HttpObserver<ArrayList<RepoCommit>>() {
            @Override
            public void onError(Throwable error) {
                mView.hideLoading();
                if(!StringUtils.isBlankList(commits)){
                    mView.showErrorToast(getErrorTip(error));
                } else if(error instanceof HttpPageNoFoundError){
                    mView.showCommits(new ArrayList<RepoCommit>());
                }else{
                    mView.showLoadError(getErrorTip(error));
                }
            }

            @Override
            public void onSuccess(HttpResponse<ArrayList<RepoCommit>> response) {
                mView.hideLoading();
                if(commits == null || isReload || readCacheFirst){
                    commits = response.body();
                } else {
                    commits.addAll(response.body());
                }
                if(response.body().size() == 0 && commits.size() != 0){
                    mView.setCanLoadMore(false);
                } else {
                    mView.showCommits(commits);
                }
            }
        };
        generalRxHttpExecute(new IObservableCreator<ArrayList<RepoCommit>>() {
            @Override
            public Observable<Response<ArrayList<RepoCommit>>> createObservable(boolean forceNetWork) {
                return getCommitService().getRepoCommits(forceNetWork, user, repo, branch, page);
            }
        }, httpObserver, readCacheFirst);
    }

    @Override
    public void loadCommits(boolean isReload, int page) {
        loadCommits(branch, isReload, page);
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }
}
