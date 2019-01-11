package com.syv.takecare.takecare;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import static android.view.View.VISIBLE;


public class FeedListFragment extends Fragment {
    private final static String TAG = "FeedListFragment";
    private static final int LIST_JUMP_THRESHOLD = 4;
    private static final int ICON_FILL_ITERATIONS = 12;
    private static final int ICON_FILL_DURATION = 200;
    private static final int ICON_ACTIVATED_DURATION = 400;
    private static final String RECYCLER_STATE_POSITION_KEY = "RECYCLER POSITION";
    private ReentrantLock iconLock = new ReentrantLock();
    private static final String EXTRA_ITEM_ID = "Item Id";
    private RecyclerView recyclerView;
    private View emptyFeedView;

    private AppCompatButton jumpButton;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private StorageReference storage;
    private FirebaseUser user;
    private int position = 0;

    private FirestoreRecyclerAdapter<FeedCardInformation, ItemsViewHolder> currentAdapter = null;

    private int orientation;
    private int absolutePosition;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Starting");
        View view = inflater.inflate(R.layout.fragment_feed_list,container, false);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance().getReference();
        user = auth.getCurrentUser();
        orientation = getResources().getConfiguration().orientation;
        recyclerView = (RecyclerView) view.findViewById(R.id.taker_feed_list);
        emptyFeedView = view.findViewById(R.id.empty_feed_view);
        emptyFeedView.setVisibility(View.GONE);
        setUpRecyclerView();
        jumpButton = view.findViewById(R.id.jump_button);
        jumpButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                recyclerView.smoothScrollToPosition(0);
                jumpButton.setVisibility(View.GONE);
            }
        });
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            jumpButton.setCompoundDrawablesWithIntrinsicBounds(AppCompatResources.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_arrow_back), null, null, null);
        } else {
            jumpButton.setCompoundDrawablesWithIntrinsicBounds(null, null, AppCompatResources.getDrawable(getActivity().getApplicationContext(), R.drawable.ic_arrow_up), null);
        }

        Log.d(TAG, "onCreateView: Starting taking care of savedInstanceState");
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(RECYCLER_STATE_POSITION_KEY)) {
                this.absolutePosition = savedInstanceState.getInt(RECYCLER_STATE_POSITION_KEY);
                Log.d(TAG, "onCreateView: absolute position is: " + this.absolutePosition);
            }
        }
        Log.d(TAG, "onCreateView: Ending. Absolute position is: " + absolutePosition);
        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        Log.d(TAG, "onSaveInstanceState: writing position " + absolutePosition);
        savedInstanceState.putInt(RECYCLER_STATE_POSITION_KEY, absolutePosition);
        //savedInstanceState.putParcelable("ADAPTER", (Parcelable)currentAdapter);
        Bundle recyclerViewState = new Bundle();
        Parcelable listState = recyclerView.getLayoutManager().onSaveInstanceState();
        recyclerViewState.putParcelable("KEY_RECYCLER_STATE", listState);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void setUpRecyclerView() {
        Log.d(TAG, "setUpRecyclerView: setting layout manager for the current orientation");
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext(), LinearLayoutManager.VERTICAL, false));
        }
        //Optimizing recycler view's performance:
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(10);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        setUpAdapter();

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updatePosition();
                } else {
                    jumpButton.setVisibility(View.GONE);
                }
                absolutePosition = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
            }
        });
    }

    private void setUpAdapter() {
        Log.d(TAG, "setUpAdapter: setting up adapter");
        String queryCategoriesFilter = ((TakerMenuActivity)getActivity()).getQueryCategoriesFilter();
        String queryPickupMethodFilter = ((TakerMenuActivity)getActivity()).getQueryPickupMethodFilter();
        if (currentAdapter != null)
            currentAdapter.stopListening();

        // Default: no filters
        Query query = db.collection("items")
                .whereEqualTo("status", 1)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        if (queryCategoriesFilter != null && queryPickupMethodFilter != null) {
            // Filter by categories and pickup method
            Log.d(TAG, "setUpAdapter: query has: category: " + queryCategoriesFilter + " pickup: " + queryPickupMethodFilter);
            query = db.collection("items")
                    .whereEqualTo("category", queryCategoriesFilter)
                    .whereEqualTo("pickupMethod", queryPickupMethodFilter)
                    .whereEqualTo("status", 1)
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        } else if (queryCategoriesFilter != null) {
            // Filter by categories
            Log.d(TAG, "setUpAdapter: query has: category: " + queryCategoriesFilter);
            query = db.collection("items")
                    .whereEqualTo("category", queryCategoriesFilter)
                    .whereEqualTo("status", 1)
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        } else if (queryPickupMethodFilter != null) {
            // Filter by pickup method
            Log.d(TAG, "setUpAdapter: query has: pickup: " + queryPickupMethodFilter);
            query = db.collection("items")
                    .whereEqualTo("pickupMethod", queryPickupMethodFilter)
                    .whereEqualTo("status", 1)
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        }


        FirestoreRecyclerOptions<FeedCardInformation> response = new FirestoreRecyclerOptions.Builder<FeedCardInformation>()
                .setQuery(query, FeedCardInformation.class)
                .build();

        currentAdapter = new FirestoreRecyclerAdapter<FeedCardInformation, ItemsViewHolder>(response) {

            private int focusedItem = 0;

            @Override
            public void onAttachedToRecyclerView(final RecyclerView recyclerView) {
                super.onAttachedToRecyclerView(recyclerView);

                // Handle key up and key down and attempt to move selection
                recyclerView.setOnKeyListener(new View.OnKeyListener() {
                    @Override
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();

                        // Return false if scrolled to the bounds and allow focus to move off the list
                        if (event.getAction() == KeyEvent.ACTION_DOWN) {
                            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                                return tryMoveSelection(lm, 1);
                            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                return tryMoveSelection(lm, -1);
                            }
                        }

                        return false;
                    }
                });
            }

            private boolean tryMoveSelection(RecyclerView.LayoutManager layoutManager, int direction) {
                int tryFocusItem = focusedItem + direction;

                // If still within valid bounds, move the selection, notify to redraw, and scroll
                if (tryFocusItem >= 0 && tryFocusItem < getItemCount()) {
                    notifyItemChanged(focusedItem);
                    focusedItem = tryFocusItem;
                    notifyItemChanged(focusedItem);
                    layoutManager.scrollToPosition(focusedItem);
                    return true;
                }
                return false;
            }

            @SuppressLint("ClickableViewAccessibility")
            @Override
            protected void onBindViewHolder(@NonNull final ItemsViewHolder holder, final int position, @NonNull final FeedCardInformation model) {
                // Attempt to remove item from feed if reported by the user
                final String itemId = getSnapshots().getSnapshot(holder.getAdapterPosition()).getId();
                holder.itemTitle.setText(model.getTitle());
                RequestOptions requestOptions = new RequestOptions();
                requestOptions = requestOptions.transforms(new CenterCrop(), new RoundedCorners(16));
                if (model.getPhoto() == null) {
                    holder.itemPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    switch (model.getCategory()) {
                        case "Food":
                            holder.itemPhoto.setImageDrawable(getResources().getDrawable(R.drawable.ic_pizza_slice_purple));
                            break;
                        case "Study Material":
                            holder.itemPhoto.setImageDrawable(getResources().getDrawable(R.drawable.ic_book_purple));
                            break;
                        case "Households":
                            holder.itemPhoto.setImageDrawable(getResources().getDrawable(R.drawable.ic_lamp_purple));
                            break;
                        case "Lost & Found":
                            holder.itemPhoto.setImageDrawable(getResources().getDrawable(R.drawable.ic_lost_and_found_purple));
                            break;
                        case "Hitchhikes":
                            holder.itemPhoto.setImageDrawable(getResources().getDrawable(R.drawable.ic_car_purple));
                            break;
                        default:
                            holder.itemPhoto.setImageDrawable(getResources().getDrawable(R.drawable.ic_treasure_purple));
                            break;
                    }
                    holder.itemPhoto.setScaleType(ImageView.ScaleType.CENTER);
                } else {
                    Glide.with(getActivity().getApplicationContext())
                            .load(model.getPhoto())
                            .apply(requestOptions)
                            .into(holder.itemPhoto);
                }

                // Category selection
                int categoryId;
                switch (model.getCategory()) {
                    case "Food":
                        categoryId = R.drawable.ic_pizza_slice_purple;
                        break;
                    case "Study Material":
                        categoryId = R.drawable.ic_book_purple;
                        break;
                    case "Households":
                        categoryId = R.drawable.ic_lamp_purple;
                        break;
                    case "Lost & Found":
                        categoryId = R.drawable.ic_lost_and_found_purple;
                        break;
                    case "Hitchhikes":
                        categoryId = R.drawable.ic_car_purple;
                        break;
                    default:
                        categoryId = R.drawable.ic_treasure_purple;
                        break;
                }

                int pickupMethodId;
                switch (model.getPickupMethod()) {
                    case "In Person":
                        pickupMethodId = R.drawable.ic_in_person_purple;
                        break;
                    case "Giveaway":
                        pickupMethodId = R.drawable.ic_giveaway_purple;
                        break;
                    default:
                        pickupMethodId = R.drawable.ic_race_purple;
                        break;
                }

                holder.profilePhoto.setImageResource(R.drawable.ic_user_purple);
                db.collection("users").document(model.getPublisher())
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                holder.itemPublisher.setText(documentSnapshot.getString("name"));
                                if (documentSnapshot.getString("profilePicture") != null) {
                                    if(getActivity() == null){
                                        return;
                                    }
                                    Glide.with(getActivity().getApplicationContext())
                                            .load(documentSnapshot.getString("profilePicture"))
                                            .apply(RequestOptions.circleCropTransform())
                                            .into(holder.profilePhoto);
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {

                            }
                        });
                holder.itemCategory.setImageResource(categoryId);
                holder.itemPickupMethod.setImageResource(pickupMethodId);

                activateViewHolderIcons(holder, model);

                holder.card.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getActivity().getApplicationContext(), ItemInfoActivity.class);
                        intent.putExtra(EXTRA_ITEM_ID, itemId);
                        intent.putExtra(Intent.EXTRA_UID, user.getUid());
                        startActivity(intent);
                    }
                });

                holder.itemReport.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopupMenu menu = new PopupMenu(getActivity().getApplicationContext(), v);
                        menu.getMenuInflater().inflate(R.menu.report_menu, menu.getMenu());
                        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                // Using HTML tags for bold substrings inside the alert message
                                String msg, warning;
                                Spanned alertMsg;
                                switch (item.getItemId()) {
                                    case R.id.report_inappropriate:
                                        //TODO: add logic to this report reason
                                        msg = getString(R.string.report_inappropriate_alert);
                                        warning = "<b><small><i>" + getString(R.string.report_alert_warning) + "</i></small></b>";
                                        alertMsg = Html.fromHtml(msg + "<br><br>" + warning);
                                        showBlockAlertMessage(alertMsg, itemId, item.getItemId());
                                        break;
                                    case R.id.report_no_fit:
                                        //TODO: add logic to this report reason
                                        msg = getString(R.string.report_inappropriate_alert);
                                        warning = "<b><small>" + getString(R.string.report_alert_warning) + "</small></b>";
                                        alertMsg = Html.fromHtml(msg + "<br><br>" + warning);
                                        showBlockAlertMessage(alertMsg, itemId, item.getItemId());
                                        break;
                                    case R.id.report_spam:
                                        //TODO: add logic to this report reason
                                        msg = getString(R.string.report_spam_alert);
                                        warning = "<b><small>" + getString(R.string.report_alert_warning) + "</small></b>";
                                        alertMsg = Html.fromHtml(msg + "<br><br>" + warning);
                                        showBlockAlertMessage(alertMsg, itemId, item.getItemId());
                                        break;
                                    case R.id.report_hide:
                                        msg = getString(R.string.report_hide_alert);
                                        alertMsg = Html.fromHtml(msg);
                                        showBlockAlertMessage(alertMsg, itemId, item.getItemId());
                                        break;
                                    default:
                                        return false;
                                }
                                return true;
                            }
                        });
                        menu.show();
                    }
                });

                holder.itemView.setSelected(focusedItem == position);
            }

            @NonNull
            @Override
            public ItemsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = null;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.taker_feed_card_carousel, viewGroup, false);

                } else {
                    view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.taker_feed_card, viewGroup, false);
                }
                return new ItemsViewHolder(view);
            }

            @Override
            public void onError(FirebaseFirestoreException e) {
                Log.e("error", e.getMessage());
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();
                toggleVisibility();
                if (position == 0 && recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
                    recyclerView.scrollToPosition(0);
                }
//                if (getItemCount() == 0) {
//                    filterPopupMenu.setVisibility(View.GONE);
//                }
            }

            class AdapterViewHolder extends ItemsViewHolder {
                public AdapterViewHolder(View itemView) {
                    super(itemView);

                    // Handle item click and set the selection
                    itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Redraw the old selection and the new
                            notifyItemChanged(focusedItem);
                            focusedItem = getLayoutPosition();
                            notifyItemChanged(focusedItem);
                        }
                    });
                }
            }
        };

        Log.d(TAG, "setUpAdapter: created adapter");
        currentAdapter.notifyDataSetChanged();
        recyclerView.setAdapter(currentAdapter);
        //toggleVisibility();
        currentAdapter.startListening();
        Log.d(TAG, "setUpAdapter: done");
    }
    @SuppressLint("ClickableViewAccessibility")
    private void activateViewHolderIcons(final ItemsViewHolder holder, final FeedCardInformation model) {

        holder.itemCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (iconLock.isLocked()) {
                    return;
                }

                new Thread(new Runnable() {
                    public void run() {
                        iconLock.lock();
                        float alpha = (float) 0.6;
                        for (int i = 0; i < ICON_FILL_ITERATIONS; i++) {
                            v.setAlpha(alpha);
                            try {
                                Thread.sleep(ICON_FILL_DURATION / ICON_FILL_ITERATIONS);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            alpha += (float) (1 - 0.6) / ICON_FILL_ITERATIONS;
                        }

                        try {
                            Thread.sleep(ICON_ACTIVATED_DURATION);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        for (int i = 0; i < ICON_FILL_ITERATIONS; i++) {
                            v.setAlpha(alpha);
                            try {
                                Thread.sleep(ICON_FILL_DURATION / ICON_FILL_ITERATIONS);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            alpha -= (float) (1 - 0.6) / ICON_FILL_ITERATIONS;
                        }
                        iconLock.unlock();
                    }
                }).start();

                String str;
                switch (model.getCategory()) {
                    case "Food":
                        str = "This item's category is food";
                        break;
                    case "Study Material":
                        str = "This item's category is study material";
                        break;
                    case "Households":
                        str = "This item's category is household objects";
                        break;
                    case "Lost & Found":
                        str = "This item's category is lost&founds";
                        break;
                    case "Hitchhikes":
                        str = "This item's category is hitchhiking";
                        break;
                    default:
                        str = "This item is in a category of its own";
                        break;
                }

                Toast.makeText(getActivity().getApplicationContext(), str, Toast.LENGTH_SHORT).show();
            }
        });

        holder.itemPickupMethod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (iconLock.isLocked()) {
                    return;
                }

                new Thread(new Runnable() {
                    public void run() {
                        iconLock.lock();
                        float alpha = (float) 0.6;
                        for (int i = 0; i < ICON_FILL_ITERATIONS; i++) {
                            v.setAlpha(alpha);
                            try {
                                Thread.sleep(ICON_FILL_DURATION / ICON_FILL_ITERATIONS);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            alpha += (float) (0.9 - 0.6) / ICON_FILL_ITERATIONS;
                        }

                        try {
                            Thread.sleep(ICON_ACTIVATED_DURATION);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        for (int i = 0; i < ICON_FILL_ITERATIONS; i++) {
                            v.setAlpha(alpha);
                            try {
                                Thread.sleep(ICON_FILL_DURATION / ICON_FILL_ITERATIONS);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            alpha -= (float) (0.9 - 0.6) / ICON_FILL_ITERATIONS;
                        }
                        iconLock.unlock();
                    }
                }).start();

                String str;
                switch (model.getPickupMethod()) {
                    case "In Person":
                        str = "This item is available for pick-up in person";
                        break;
                    case "Giveaway":
                        str = "This item is available in a public giveaway";
                        break;
                    default:
                        str = "Race to get this item before anyone else!";
                        break;
                }

                Toast.makeText(getActivity().getApplicationContext(), str, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void updatePosition() {
        assert recyclerView.getLayoutManager() != null;
        position = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        Log.d(TAG, "onScrollStateChanged: POSITION IS: " + position);
        tryToggleJumpButton();
    }
    @Override
    public void onStart(){
        Log.d(TAG, "onStart: Starting.");
        currentAdapter.startListening();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "thread run: moving to " + absolutePosition);
                recyclerView.scrollToPosition(absolutePosition);
                updatePosition();
            }
        }, 300);
        super.onStart();
    }
    @Override
    public void onStop() {
        super.onStop();
        currentAdapter.stopListening();
    }
    public void toggleVisibility() {
        if (emptyFeedView != null) {
            //Make the emptyFeedView visible of the adapter has no items (feed is empty)
            emptyFeedView.setVisibility(
                    (recyclerView.getAdapter() == null || recyclerView.getAdapter().getItemCount() == 0) ?
                            VISIBLE : View.GONE);
            //The list itself is set to be invisible if there are no items, in order to display emptyFeedView in its stead
            recyclerView.setVisibility(
                    (recyclerView.getAdapter() == null || recyclerView.getAdapter().getItemCount() == 0) ?
                            View.GONE : View.VISIBLE);
        }
    }
    // TODO: add togglefiltermenu related stuff here


    private void tryToggleJumpButton() {
        if (position >= LIST_JUMP_THRESHOLD) {
            jumpButton.setVisibility(VISIBLE);
            Log.d(TAG, "tryToggleJumpButton: jump button is visible");
        } else {
            jumpButton.setVisibility(View.GONE);
        }
    }
    private void showBlockAlertMessage(final Spanned msg, final String itemId, final int cause) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(getActivity().getApplicationContext());
        builder.setTitle("Block Item")
                .setMessage(msg)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        hideItem(itemId);
                        switch (cause) {
                            //TODO: add
                            case R.id.report_inappropriate:
                                break;
                            case R.id.report_no_fit:
                                break;
                            case R.id.report_spam:
                                break;
                            default:
                                // User hides item - do nothing
                                break;
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                })
                .show();
    }
    private void hideItem(final String itemId) {
        if (user == null) {
            return;
        }
        // Atomic operation - no need for transactions!
        Log.d(TAG, "hideItem: hiding item from user");
        db.collection("items").document(itemId)
                .update("hideFrom", FieldValue.arrayUnion(user.getUid()));
    }
}