package com.socks.jiandan.ui.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.socks.jiandan.R;
import com.socks.jiandan.base.BaseFragment;
import com.socks.jiandan.callback.LoadFinishCallBack;
import com.socks.jiandan.constant.ToastMsg;
import com.socks.jiandan.model.Comment;
import com.socks.jiandan.model.Picture;
import com.socks.jiandan.model.Vote;
import com.socks.jiandan.net.Request4CommentCounts;
import com.socks.jiandan.net.Request4Picture;
import com.socks.jiandan.net.Request4Vote;
import com.socks.jiandan.ui.CommentListActivity;
import com.socks.jiandan.utils.ShareUtil;
import com.socks.jiandan.utils.ShowToast;
import com.socks.jiandan.utils.String2TimeUtil;
import com.socks.jiandan.view.AutoLoadRecyclerView;
import com.socks.jiandan.view.googleprogressbar.GoogleProgressBar;
import com.socks.jiandan.view.matchview.MatchTextView;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * 段子碎片
 *
 * @author zhaokaiqiang
 */
public class PictureFragment extends BaseFragment {

	@InjectView(R.id.recycler_view)
	AutoLoadRecyclerView mRecyclerView;
	@InjectView(R.id.swipeRefreshLayout)
	SwipeRefreshLayout mSwipeRefreshLayout;
	@InjectView(R.id.google_progress)
	GoogleProgressBar google_progress;
	@InjectView(R.id.tv_error)
	MatchTextView tv_error;

	private PictureAdapter mAdapter;
	private LoadFinishCallBack mLoadFinisCallBack;

	public PictureFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		mActionBar.setTitle("无聊图");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_joke, container, false);
		ButterKnife.inject(this, view);

		mRecyclerView.setHasFixedSize(false);
		mRecyclerView.setItemAnimator(new DefaultItemAnimator());
		mLoadFinisCallBack = mRecyclerView;
		mRecyclerView.setLoadMoreListener(new AutoLoadRecyclerView.onLoadMoreListener() {
			@Override
			public void loadMore() {
				mAdapter.loadNextPage();
			}
		});

		mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
				android.R.color.holo_green_light,
				android.R.color.holo_orange_light,
				android.R.color.holo_red_light);

		mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				mAdapter.loadFirst();
			}
		});

		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mAdapter = new PictureAdapter();
		mRecyclerView.setAdapter(mAdapter);
		mAdapter.loadFirst();

	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_joke, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.action_refresh) {
			mSwipeRefreshLayout.setRefreshing(true);
			mAdapter.loadFirst();
			return true;
		}

		return false;
	}

	@Override
	public void onActionBarClick() {
		if (mRecyclerView != null && mAdapter.mJokes.size() > 0) {
			mRecyclerView.scrollToPosition(0);
		}
	}

	public class PictureAdapter extends RecyclerView.Adapter<ViewHolder> {

		private int page;
		private ArrayList<Picture> mJokes;

		public PictureAdapter() {
			mJokes = new ArrayList<Picture>();
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent,
		                                     int viewType) {
			View v = LayoutInflater.from(parent.getContext())
					.inflate(R.layout.item_pic, parent, false);
			return new ViewHolder(v);
		}

		@Override
		public void onBindViewHolder(final ViewHolder holder, final int position) {

			final Picture picture = mJokes.get(position);
			holder.tv_content.setText(picture.getText_content().trim());
			holder.tv_author.setText(picture.getComment_author());
			holder.tv_time.setText(String2TimeUtil.dateString2GoodExperienceFormat(picture.getComment_date()));
			holder.tv_like.setText(picture.getVote_positive());
			holder.tv_comment_count.setText(picture.getComment_counts());

			//用于恢复默认的文字
			holder.tv_like.setTypeface(Typeface.DEFAULT);
			holder.tv_like.setTextColor(getResources().getColor(R.color
					.secondary_text_default_material_light));
			holder.tv_support_des.setTextColor(getResources().getColor(R.color
					.secondary_text_default_material_light));

			holder.tv_unlike.setText(picture.getVote_negative());
			holder.tv_unlike.setTypeface(Typeface.DEFAULT);
			holder.tv_unlike.setTextColor(getResources().getColor(R.color
					.secondary_text_default_material_light));
			holder.tv_unsupport_des.setTextColor(getResources().getColor(R.color
					.secondary_text_default_material_light));

			holder.img_share.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					new MaterialDialog.Builder(getActivity())
							.items(R.array.joke_dialog)
							.itemsCallback(new MaterialDialog.ListCallback() {
								@Override
								public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {

									switch (which) {
										//分享
										case 0:
											ShareUtil.shareText(getActivity(), picture.getComment_content());
											break;
										//复制
										case 1:
											ClipboardManager clip = (ClipboardManager)
													getActivity().getSystemService(Context
															.CLIPBOARD_SERVICE);
											clip.setPrimaryClip(ClipData.newPlainText
													(null, picture.getComment_content()));
											ShowToast.Short(ToastMsg.COPY_SUCCESS);
											break;
									}

								}
							})
							.show();
				}
			});

			holder.ll_support.setOnClickListener(new onVoteClickListener(picture.getComment_ID(),
					Vote.OO, holder, picture));

			holder.ll_unsupport.setOnClickListener(new onVoteClickListener(picture.getComment_ID(),
					Vote.XX, holder, picture));

			holder.ll_comment.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {

					Intent intent = new Intent(getActivity(), CommentListActivity.class);
					intent.putExtra("thread_key", "comment-" + picture.getComment_ID());
					startActivity(intent);
				}
			});

		}

		/**
		 * 投票监听器
		 */
		private class onVoteClickListener implements View.OnClickListener {

			private String comment_ID;
			private String tyle;
			private ViewHolder holder;
			private Picture picture;

			public onVoteClickListener(String comment_ID, String tyle, ViewHolder holder, Picture joke) {
				this.comment_ID = comment_ID;
				this.tyle = tyle;
				this.holder = holder;
				this.picture = joke;
			}

			@Override
			public void onClick(View v) {

				//避免快速点击，造成大量网络访问
				if (holder.isClickFinish) {
					vote(comment_ID, tyle, holder, picture);
					holder.isClickFinish = false;
				}

			}
		}

		/**
		 * 投票
		 *
		 * @param comment_ID
		 */
		public void vote(String comment_ID, String tyle, final ViewHolder holder, final Picture picture) {

			String url;

			if (tyle.equals(Vote.XX)) {
				url = Vote.getXXUrl(comment_ID);
			} else {
				url = Vote.getOOUrl(comment_ID);
			}

			executeRequest(new Request4Vote(url, new
					Response.Listener<Vote>() {
						@Override
						public void onResponse(Vote response) {

							holder.isClickFinish = true;
							String result = response.getResult();

							if (result.equals(Vote.RESULT_OO_SUCCESS)) {
								ShowToast.Short("顶的好舒服~");
								//变红+1
								int vote = Integer.valueOf(picture.getVote_positive());
								picture.setVote_positive((vote + 1) + "");
								holder.tv_like.setText(picture.getVote_positive());
								holder.tv_like.setTypeface(Typeface.DEFAULT_BOLD);
								holder.tv_like.setTextColor(getResources().getColor
										(android.R.color.holo_red_light));
								holder.tv_support_des.setTextColor(getResources().getColor
										(android.R.color.holo_red_light));

							} else if (result.equals(Vote.RESULT_XX_SUCCESS)) {
								ShowToast.Short("疼...轻点插");
								//变绿+1
								int vote = Integer.valueOf(picture.getVote_negative());
								picture.setVote_negative((vote + 1) + "");
								holder.tv_unlike.setText(picture.getVote_negative());
								holder.tv_unlike.setTypeface(Typeface.DEFAULT_BOLD);
								holder.tv_unlike.setTextColor(getResources().getColor
										(android.R.color.holo_green_light));
								holder.tv_unsupport_des.setTextColor(getResources().getColor
										(android.R.color.holo_green_light));

							} else if (result.equals(Vote.RESULT_HAVE_VOTED)) {
								ShowToast.Short("投过票了");
							} else {
								ShowToast.Short("卧槽，发生了什么！");
							}

						}
					}, new Response.ErrorListener() {
				@Override
				public void onErrorResponse(VolleyError error) {
					ShowToast.Short(ToastMsg.VOTE_FAILED);
					holder.isClickFinish = true;
				}
			}));
		}


		@Override
		public int getItemCount() {
			return mJokes.size();
		}

		public void loadFirst() {
			page = 1;
			loadData();
		}

		public void loadNextPage() {
			page++;
			loadData();
		}

		private void loadData() {
			executeRequest(new Request4Picture(Picture.getRequestUrl(page),
					new Response.Listener<ArrayList<Picture>>
							() {
						@Override
						public void onResponse(ArrayList<Picture> response) {
							getCommentCounts(response);
						}
					}, new Response.ErrorListener() {
				@Override
				public void onErrorResponse(VolleyError error) {

					tv_error.setVisibility(View.VISIBLE);
					google_progress.setVisibility(View.GONE);
					mLoadFinisCallBack.loadFinish(null);
					if (mSwipeRefreshLayout.isRefreshing()) {
						mSwipeRefreshLayout.setRefreshing(false);
					}
				}
			}));
		}

		//获取评论数量
		private void getCommentCounts(final ArrayList<Picture> pictures) {

			StringBuilder sb = new StringBuilder();
			for (Picture joke : pictures) {
				sb.append("comment-" + joke.getComment_ID() + ",");
			}

			executeRequest(new Request4CommentCounts(Comment.getCommentCountsURL(sb.toString()), new Response
					.Listener<ArrayList<Comment>>() {

				@Override
				public void onResponse(ArrayList<Comment> response) {

					google_progress.setVisibility(View.GONE);
					tv_error.setVisibility(View.GONE);

					for (int i = 0; i < pictures.size(); i++) {
						pictures.get(i).setComment_counts(response.get(i).getComments() + "");
					}

					if (page == 1) {
						mJokes.clear();
						mJokes.addAll(pictures);
					} else {
						mJokes.addAll(pictures);
					}

					notifyDataSetChanged();

					if (mSwipeRefreshLayout.isRefreshing()) {
						mSwipeRefreshLayout.setRefreshing(false);
					}

					mLoadFinisCallBack.loadFinish(null);
				}
			}, new Response.ErrorListener() {
				@Override
				public void onErrorResponse(VolleyError error) {
					mLoadFinisCallBack.loadFinish(null);
					tv_error.setVisibility(View.VISIBLE);
					google_progress.setVisibility(View.GONE);
					if (mSwipeRefreshLayout.isRefreshing()) {
						mSwipeRefreshLayout.setRefreshing(false);
					}

				}
			}
			));

		}

	}

	public static class ViewHolder extends RecyclerView.ViewHolder {

		private TextView tv_author;
		private TextView tv_time;
		private TextView tv_content;
		private TextView tv_like;
		private TextView tv_unlike;
		private TextView tv_comment_count;
		private TextView tv_unsupport_des;
		private TextView tv_support_des;

		private ImageView img_share;

		private LinearLayout ll_support;
		private LinearLayout ll_unsupport;
		private LinearLayout ll_comment;
		//用于处理多次点击造成的网络访问
		private boolean isClickFinish;

		public ViewHolder(View contentView) {
			super(contentView);

			isClickFinish = true;

			tv_author = (TextView) contentView.findViewById(R.id.tv_author);
			tv_content = (TextView) contentView.findViewById(R.id.tv_content);
			tv_time = (TextView) contentView.findViewById(R.id.tv_time);
			tv_like = (TextView) contentView.findViewById(R.id.tv_like);
			tv_unlike = (TextView) contentView.findViewById(R.id.tv_unlike);
			tv_comment_count = (TextView) contentView.findViewById(R.id.tv_comment_count);
			tv_unsupport_des = (TextView) contentView.findViewById(R.id.tv_unsupport_des);
			tv_support_des = (TextView) contentView.findViewById(R.id.tv_support_des);

			img_share = (ImageView) contentView.findViewById(R.id.img_share);

			ll_support = (LinearLayout) contentView.findViewById(R.id.ll_support);
			ll_unsupport = (LinearLayout) contentView.findViewById(R.id.ll_unsupport);
			ll_comment = (LinearLayout) contentView.findViewById(R.id.ll_comment);

		}
	}
}
