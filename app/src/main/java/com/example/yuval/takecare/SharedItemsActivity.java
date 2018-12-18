package com.example.yuval.takecare;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class SharedItemsActivity extends AppCompatActivity {

    private static final String TAG = "Shared Items";
    private static final int LIST_JUMP_THRESHOLD = 4;
    private FeedRecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private StorageReference storage;
    private int position = 0;
    private AppCompatButton jumpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_items);

        Toolbar toolbar = (Toolbar) findViewById(R.id.shared_items_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        jumpButton = findViewById(R.id.shared_items_jump_button);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance().getReference();
        setUpRecyclerView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:
                intent = new Intent(this, TakerMenuActivity.class);
                startActivity(intent);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setUpRecyclerView() {
        recyclerView = (FeedRecyclerView) findViewById(R.id.taker_feed_list);
        View emptyFeedView = findViewById(R.id.empty_feed_view);
        //Optimizing recycler view's performance:
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(10);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false));

        final FirebaseUser user = auth.getCurrentUser();
        assert user != null;

        Query query = db.collection("users").document(user.getUid()).collection("publishedItems")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<FeedCardInformation> response = new FirestoreRecyclerOptions.Builder<FeedCardInformation>()
                .setQuery(query, FeedCardInformation.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<FeedCardInformation, ItemsViewHolder>(response) {
            @Override
            protected void onBindViewHolder(@NonNull final ItemsViewHolder holder, int position, @NonNull FeedCardInformation model) {
                Log.d(TAG, "model " + model.getPhoto());
                holder.itemTitle.setText(model.getTitle());
                RequestOptions requestOptions = new RequestOptions();
                requestOptions = requestOptions.transforms(new CenterCrop(), new RoundedCorners(16));
                Glide.with(holder.card)
                        .load(model.getPhoto())
                        .apply(requestOptions)
                        .into(holder.itemPhoto);

                // category selection
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

                holder.itemPublisher.setText(R.string.user_name);
                holder.profilePhoto.setImageResource(R.drawable.ic_user_purple);
                db.collection("users").document(model.getPublisher())
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                Log.d(TAG, "Found the user: " + documentSnapshot);
                                holder.itemPublisher.setText(documentSnapshot.getString("name"));
                                if (documentSnapshot.getString("profilePicture") != null) {
                                    Glide.with(holder.card)
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
                holder.itemPublisher.setText(model.getPublisher());
                holder.itemCategory.setImageResource(categoryId);
                holder.itemPickupMethod.setImageResource(pickupMethodId);
                holder.itemCategory.setTag(categoryId);
                holder.itemPickupMethod.setTag(pickupMethodId);
            }

            @NonNull
            @Override
            public ItemsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.taker_feed_card, viewGroup, false);
                return new ItemsViewHolder(view);
            }

            @Override
            public void onError(FirebaseFirestoreException e) {
                Log.e("error", e.getMessage());
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (position == 0) {
                    recyclerView.scrollToPosition(0);
                }
            }
        };

        recyclerView.setEmptyView(emptyFeedView);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updatePosition();
                }
            }
        });
    }

    private void updatePosition() {
        position = ((LinearLayoutManager) recyclerView.getLayoutManager())
                .findFirstVisibleItemPosition();
        Log.d(TAG, "onScrollStateChanged: POSITION IS: " + position);
        tryToggleJumpButton();
    }

    private void tryToggleJumpButton() {
        if (position >= LIST_JUMP_THRESHOLD) {
            jumpButton.setVisibility(View.VISIBLE);
            Log.d(TAG, "tryToggleJumpButton: jump button is visible");
        } else {
            jumpButton.setVisibility(View.GONE);
        }
    }

    public void onJumpClick(View view) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        assert layoutManager != null;
        recyclerView.smoothScrollToPosition(0);
        jumpButton.setVisibility(View.GONE);
    }
}
