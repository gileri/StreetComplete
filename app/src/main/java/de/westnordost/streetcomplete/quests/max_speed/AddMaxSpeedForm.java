package de.westnordost.streetcomplete.quests.max_speed;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import de.westnordost.streetcomplete.R;
import de.westnordost.streetcomplete.quests.AbstractQuestFormAnswerFragment;
import de.westnordost.streetcomplete.quests.ImageListQuestAnswerFragment;
import de.westnordost.streetcomplete.quests.QuestUtil;
import de.westnordost.streetcomplete.quests.max_speed.model.MaxSpeed;
import de.westnordost.streetcomplete.view.ImageSelectAdapter;
import de.westnordost.streetcomplete.view.Item;
import de.westnordost.streetcomplete.view.dialogs.AlertDialogBuilder;

import static android.view.Menu.NONE;

public class AddMaxSpeedForm extends ImageListQuestAnswerFragment implements ImageSelectAdapter.OnItemSelectionListener
{
	public static final Item
		MAX_SPEED = new Item("maxspeed", R.drawable.ic_quest_max_speed, R.string.quest_maxspeed_type_explicit),
		ADVISORY_SPEED = new Item("advisory_speed", R.drawable.recycling_container_underground, R.string.underground_recycling_container),
		MAX_SPEED_IMPLICIT_COUNTRY = new Item("maxspeed_country", R.drawable.recycling_centre, R.string.recycling_centre),
		MAX_SPEED_IMPLICIT_ROADTYPE = new Item("maxspeed_roadtype", R.drawable.recycling_centre, R.string.recycling_centre),
		LIVING_STREET = new Item("living_street", R.drawable.recycling_centre, R.string.recycling_centre);

	private static final String	IS_ADVISORY_SPEED_LIMIT = "is_advisory_speed_limit";

	private static final Collection<String>
			URBAN_OR_RURAL_ROADS = Arrays.asList("primary","secondary","tertiary","unclassified",
					"primary_link","secondary_link","tertiary_link","road"),
			ROADS_WITH_DEFINITE_SPEED_LIMIT = Arrays.asList("trunk","motorway","living_street"),
			POSSIBLY_SLOWZONE_ROADS = Arrays.asList("residential","unclassified"),
			MAYBE_LIVING_STREET = Arrays.asList("residential");

	private EditText speedInput;
	private CheckBox zoneCheckbox;
	private Spinner speedUnitSelect;

	private boolean isAdvisorySpeedLimit;

	@Override
	protected Item[] getItems() {
		return new Item[]{MAX_SPEED, ADVISORY_SPEED, MAX_SPEED_IMPLICIT_COUNTRY, MAX_SPEED_IMPLICIT_ROADTYPE, LIVING_STREET};
	}

	@Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
									   Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		this.imageSelector.setOnItemSelectionListener(this);
		return view;
	}

	private void setStreetSignLayout(int resourceId)
	{
		View contentView = setContentView(getCurrentCountryResources().getLayout(resourceId));
		TextView title = this.getView().findViewById(R.id.title);
		title.setText(R.string.quest_maxspeed_name_explicit);

		speedInput = contentView.findViewById(R.id.maxSpeedInput);

		speedUnitSelect = contentView.findViewById(R.id.speedUnitSelect);
		initSpeedUnitSelect();
	}

	private void initSpeedUnitSelect()
	{
		List<String> speedUnits = getCountryInfo().getSpeedUnits();
		speedUnitSelect.setVisibility(speedUnits.size() == 1 ? View.GONE : View.VISIBLE);
		speedUnitSelect.setAdapter(new ArrayAdapter<>(getContext(), R.layout.spinner_item_centered, speedUnits));
		speedUnitSelect.setSelection(0);
	}

	private void initZoneCheckbox(View zoneContainer)
	{
		boolean isResidential = POSSIBLY_SLOWZONE_ROADS.contains(getOsmElement().getTags().get("highway"));
		boolean isSlowZoneKnown = getCountryInfo().isSlowZoneKnown();
		zoneContainer.setVisibility(isSlowZoneKnown && isResidential ? View.VISIBLE : View.GONE);

		zoneCheckbox = zoneContainer.findViewById(R.id.zoneCheckbox);
		zoneCheckbox.setOnCheckedChangeListener((buttonView, isChecked) ->
		{
			if(isChecked && speedInput.getText().toString().isEmpty())
			{
				// prefill speed input with normal "slow zone" value
				String speedUnit = (String) speedUnitSelect.getSelectedItem();
				boolean isMph = speedUnit.equals("mph");
				speedInput.setText(isMph ? "20" : "30");
			}
		});

		zoneContainer.findViewById(R.id.zoneImg).setOnClickListener(v -> zoneCheckbox.toggle());
	}

	private void determineImplicitMaxspeedType()
	{
		if(getCountryInfo().getCountryCode().equals("GB"))
		{
			determineLit(() -> applyNoSignAnswer("nsl_restricted"), () -> askSingleOrDualCarriageway());
		}
		else
		{
			askUrbanOrRural();
		}
	}

	private void confirmLivingStreet(final Runnable callback)
	{
		View view = LayoutInflater.from(getActivity()).inflate(R.layout.quest_maxspeed_living_street_confirmation, null, false);

		ImageView img = view.findViewById(R.id.imgLivingStreet);
		img.setImageDrawable(getCurrentCountryResources().getDrawable(R.drawable.ic_living_street));

		new AlertDialogBuilder(getActivity())
				.setView(view)
				.setTitle(R.string.quest_maxspeed_answer_living_street_confirmation_title)
				.setPositiveButton(R.string.quest_generic_confirmation_yes, (dialog, which) -> callback.run())
				.setNegativeButton(R.string.quest_generic_confirmation_no, null)
				.show();
	}

	private void confirmNoSignSlowZone(final Runnable callback)
	{
		View view = LayoutInflater.from(getActivity()).inflate(R.layout.quest_maxspeed_no_sign_no_slow_zone_confirmation, null, false);

		ImageView imgSlowZone = view.findViewById(R.id.imgSlowZone);
		ImageView mainLayoutImgSlowZone = getView() != null ? (ImageView) getView().findViewById(R.id.zoneImg) : null;
		if(mainLayoutImgSlowZone != null)
		{
			Drawable slowZoneDrawable = mainLayoutImgSlowZone.getDrawable();
			imgSlowZone.setImageDrawable(slowZoneDrawable);
		}

		new AlertDialogBuilder(getActivity())
				.setView(view)
				.setTitle(R.string.quest_maxspeed_answer_noSign_confirmation_title)
				.setPositiveButton(R.string.quest_maxspeed_answer_noSign_confirmation_positive, (dialog, which) -> callback.run())
				.setNegativeButton(R.string.quest_generic_confirmation_no, null)
				.show();
	}

	private void askUrbanOrRural()
	{
		new AlertDialogBuilder(getActivity())
				.setTitle(R.string.quest_maxspeed_answer_noSign_info_urbanOrRural)
				.setMessage(R.string.quest_maxspeed_answer_noSign_urbanOrRural_description)
				.setPositiveButton(R.string.quest_maxspeed_answer_noSign_urbanOk, (dialog, which) -> applyNoSignAnswer("urban"))
				.setNeutralButton(R.string.quest_maxspeed_answer_noSign_ruralOk, (dialog, which) -> applyNoSignAnswer("rural"))
				.show();
	}

	private void determineLit(Runnable onYes, Runnable onNo)
	{
		String lit = getOsmElement().getTags().get("lit");
		if("yes".equals(lit)) onYes.run();
		else if("no".equals(lit)) onNo.run();
		else askLit(onYes, onNo);
	}

	private void askLit(Runnable onYes, Runnable onNo)
	{
		new AlertDialogBuilder(getActivity())
			.setMessage(R.string.quest_way_lit_road_title)
			.setPositiveButton(R.string.quest_generic_hasFeature_yes, (dialog, which) -> onYes.run())
			.setNegativeButton(R.string.quest_generic_hasFeature_no, (dialog, which) -> onNo.run())
			.show();
	}

	private void askSingleOrDualCarriageway()
	{
		new AlertDialogBuilder(getActivity())
				.setMessage(R.string.quest_maxspeed_answer_noSign_singleOrDualCarriageway_description)
				.setPositiveButton(R.string.quest_generic_hasFeature_yes, (dialog, which) -> applyNoSignAnswer("nsl_dual"))
				.setNegativeButton(R.string.quest_generic_hasFeature_no, (dialog, which) -> applyNoSignAnswer("nsl_single"))
				.show();
	}

	private void confirmNoSign(final Runnable callback)
	{
		new AlertDialogBuilder(getActivity())
				.setTitle(R.string.quest_maxspeed_answer_noSign_confirmation_title)
				.setMessage(R.string.quest_maxspeed_answer_noSign_confirmation)
				.setPositiveButton(R.string.quest_maxspeed_answer_noSign_confirmation_positive, (dialog, which) -> callback.run())
				.setNegativeButton(R.string.quest_generic_confirmation_no, null)
				.show();
	}

	private void applyNoSignAnswer(String roadType)
	{
		Bundle answer = new Bundle();
		String countryCode = getCountryInfo().getCountryCode();
		answer.putString("coucou", countryCode);
		applyImmediateAnswer(answer);
	}

	@Override protected void onClickOk()
	{
		if(!hasChanges())
		{
			Toast.makeText(getActivity(), R.string.no_changes, Toast.LENGTH_SHORT).show();
			return;
		}

		if(userSelectedUnrealisticSpeedLimit())
		{
			confirmUnusualInput(this::applySpeedLimitFormAnswer);
		}
		else
		{
			applySpeedLimitFormAnswer();
		}
	}

	private boolean userSelectedUnrealisticSpeedLimit()
	{
		int speed = Integer.parseInt(speedInput.getText().toString());
		String speedUnit = (String) speedUnitSelect.getSelectedItem();
		double speedInKmh = speedUnit.equals("mph") ? mphToKmh(speed) : speed;
		return speedInKmh > 140 || speed > 20 && speed % 5 != 0;
	}

	private static double mphToKmh(double mph)
	{
		return 1.60934 * mph;
	}

	private void applySpeedLimitFormAnswer()
	{
		Bundle answer = new Bundle();

		StringBuilder speedStr = new StringBuilder();
		int speed = Integer.parseInt(speedInput.getText().toString());
		speedStr.append(speed);

		// km/h is the OSM default, does not need to be mentioned
		String speedUnit = (String) speedUnitSelect.getSelectedItem();
		if(!speedUnit.equals("km/h"))
		{
			speedStr.append(" " + speedUnit);
		}

		applyFormAnswer(answer);
	}

	private void confirmUnusualInput(final Runnable callback)
	{
		new AlertDialogBuilder(getActivity())
				.setTitle(R.string.quest_generic_confirmation_title)
				.setMessage(R.string.quest_maxspeed_unusualInput_confirmation_description)
				.setPositiveButton(R.string.quest_generic_confirmation_yes, (dialog, which) -> callback.run())
				.setNegativeButton(R.string.quest_generic_confirmation_no, null)
				.show();
	}

	@Override public boolean hasChanges()
	{
		return !speedInput.getText().toString().isEmpty();
	}

	@Override
	public void onIndexSelected(int index) {
		SpeedInputDialog.show(this.getContext(), new OnSpeedInputListener(this.imageSelector));
	}
	private class OnSpeedInputListener implements SpeedInputDialog.OnSpeedInputListener {

		ImageSelectAdapter imageSelector;

		public OnSpeedInputListener (ImageSelectAdapter imageSelector) {
			this.imageSelector = imageSelector;
		}

		public void onSpeedInput(MaxSpeed maxspeed) {
			maxspeed.toString();
		}

		public void onCancel() {
			this.imageSelector.deselectIndex(this.imageSelector.getSelectedIndices().get(0));
		}
	}

	@Override
	public void onIndexDeselected(int index) {

	}
}
