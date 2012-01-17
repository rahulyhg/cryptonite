/*
  Based on android-file-dialog
  http://code.google.com/p/android-file-dialog/
  alexander.ponomarev.1@gmail.com
  New BSD License
*/

package csh.cryptonite;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class FileDialog extends ListActivity {

    private static final String ITEM_KEY = "key";
    private static final String ITEM_IMAGE = "image";
    private static final String ITEM_CHECK = "check";
    private static final String ITEM_FILE = "file";
    private static final String ROOT = "/";

    public static final String START_PATH = "START_PATH";
    public static final String RESULT_PATH = "RESULT_PATH";
    public static final String SELECTION_MODE = "SELECTION_MODE";
    public static final String LABEL = "LABEL";
    public static final String BUTTON_LABEL = "BUTTON_LABEL";
    public static final String CURRENT_ROOT = "CURRENT_ROOT";
    public static final String CURRENT_ROOT_NAME = "CURRENT_ROOT_NAME";

    private String currentRoot = ROOT;
    private String currentRootName = ROOT;
    private List<String> path = null;
    private TextView myPath;
    private EditText mFileName;
    private ArrayList<HashMap<String, Object>> mList;

    private Button selectButton;

    private LinearLayout layoutSelect;
    private LinearLayout layoutCreate;
    private InputMethodManager inputManager;
    private String parentPath;
    private String currentPath = currentRoot;
    
    @SuppressWarnings("unused")
    private int selectionMode = SelectionMode.MODE_OPEN;

    @SuppressWarnings("unused")
    private File selectedFile;
    private HashMap<String, Integer> lastPositions = new HashMap<String, Integer>();

    private Set<File> selectedPaths = new HashSet<File>();
    
    /** Called when the activity is first created. */
    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED, getIntent());

        setContentView(R.layout.file_dialog_main);
        myPath = (TextView) findViewById(R.id.path);
        mFileName = (EditText) findViewById(R.id.fdEditTextFile);

        inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        String currentRoot = getIntent().getStringExtra(CURRENT_ROOT);
        if (currentRoot == null) {
            currentRoot = ROOT;
        }
        currentPath = currentRoot;

        String currentRootName = getIntent().getStringExtra(CURRENT_ROOT_NAME);
        if (currentRootName == null) {
            currentRootName = currentRoot;
        }

        String buttonLabel = getIntent().getStringExtra(BUTTON_LABEL);
        selectButton = (Button) findViewById(R.id.fdButtonSelect);
        selectButton.setEnabled(true);
        selectButton.setText(buttonLabel);
        selectButton.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    /*
                      if (selectedFile != null) {
                      getIntent().putExtra(RESULT_PATH, selectedFile.getPath());
                      setResult(RESULT_OK, getIntent());
                      finish();
                      } else {
                    */
                    /* get current path */
                    if (selectionMode != SelectionMode.MODE_OPEN_MULTISELECT) {
                        if (currentPath != null) {
                            getIntent().putExtra(RESULT_PATH, currentPath);
                            setResult(RESULT_OK, getIntent());
                            finish();
                        }
                    } else {
                        getIntent().putExtra(RESULT_PATH, selectedPaths.toArray());
                        setResult(RESULT_OK, getIntent());
                        finish();
                    }
                    /* } */
                }
            });
        selectionMode = getIntent().getIntExtra(SELECTION_MODE, SelectionMode.MODE_OPEN);
        Log.v(Cryptonite.TAG, "Selection mode is " + selectionMode);
        /*
          final Button newButton = (Button) findViewById(R.id.fdButtonNew);
          newButton.setOnClickListener(new OnClickListener() {

          @Override
          public void onClick(View v) {
          setCreateVisible(v);

          mFileName.setText("");
          mFileName.requestFocus();
          }
          });

          if (selectionMode == SelectionMode.MODE_OPEN) {
          newButton.setEnabled(false);
          }
        */
        layoutSelect = (LinearLayout) findViewById(R.id.fdLinearLayoutSelect);
        layoutCreate = (LinearLayout) findViewById(R.id.fdLinearLayoutCreate);
        layoutCreate.setVisibility(View.GONE);

        final Button cancelButton = (Button) findViewById(R.id.fdButtonCancel);
        cancelButton.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    setResult(RESULT_CANCELED, getIntent());
                    finish();
                }

            });
        final Button createButton = (Button) findViewById(R.id.fdButtonCreate);
        createButton.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    if (mFileName.getText().length() > 0) {
                        getIntent().putExtra(RESULT_PATH,
                                             currentPath + "/" + mFileName.getText());
                        setResult(RESULT_OK, getIntent());
                        finish();
                    }
                }
            });

        String startPath = getIntent().getStringExtra(START_PATH);
        if (startPath != null) {
            getDir(startPath, currentRoot, currentRootName);
        } else {
            getDir(currentRoot, currentRoot, currentRootName);
        }
        String label = getIntent().getStringExtra(LABEL);
        this.setTitle(label);
    }

    private void getDir(String dirPath, String rootPath, String rootName) {

        boolean useAutoSelection = dirPath.length() < currentPath.length();

        Integer position = lastPositions.get(parentPath);

        getDirImpl(dirPath, rootPath, rootName);

        if (position != null && useAutoSelection) {
            getListView().setSelection(position);
        }

    }

    private void getDirImpl(final String dirPath, final String rootPath, final String rootName) {

        currentPath = dirPath;
        currentRoot = rootPath;
        currentRootName = rootName;
        
        final List<String> item = new ArrayList<String>();
        path = new ArrayList<String>();
        mList = new ArrayList<HashMap<String, Object>>();
        
        File f = new File(currentPath);
        File[] files = f.listFiles();
        if (files == null) {
            Log.v(Cryptonite.TAG, "No files in current path");
            currentPath = currentRoot;
            f = new File(currentPath);
            files = f.listFiles();
        }
        myPath.setText(getText(R.string.location) + ": " + currentRootName +
                       currentPath.substring(currentRoot.length()));

        if (!currentPath.equals(currentRoot)) {

            item.add(currentRoot);
            addItem(new File(currentRoot), R.drawable.ic_launcher_folder, currentRootName);
            path.add(currentRoot);

            item.add("../");
            addItem(new File(f.getParent()), R.drawable.ic_launcher_folder, "../");
            path.add(f.getParent());
            parentPath = f.getParent();

        }

        TreeMap<String, String> dirsMap = new TreeMap<String, String>();
        TreeMap<String, String> dirsPathMap = new TreeMap<String, String>();
        TreeMap<String, String> filesMap = new TreeMap<String, String>();
        TreeMap<String, String> filesPathMap = new TreeMap<String, String>();

        /* getPath() returns full path including file name */
        for (File file : files) {
            if (file.isDirectory()) {
                String dirName = file.getName();
                dirsMap.put(dirName, dirName);
                dirsPathMap.put(dirName, file.getPath());
            } else {
                filesMap.put(file.getName(), file.getName());
                filesPathMap.put(file.getName(), file.getPath());
            }
        }

        item.addAll(dirsMap.tailMap("").values());
        item.addAll(filesMap.tailMap("").values());
        path.addAll(dirsPathMap.tailMap("").values());
        path.addAll(filesPathMap.tailMap("").values());

        // for (String dir : dirsMap.tailMap("").values()) {
        //     addItem(dir, R.drawable.ic_launcher_folder, false, new File(dir));
        // }
        
        for (String dirpath : dirsPathMap.tailMap("").keySet()) {
            addItem(new File(dirsPathMap.tailMap("").get(dirpath)),
                    R.drawable.ic_launcher_folder);
        }

        for (String filepath : filesPathMap.tailMap("").keySet()) {
            addItem(new File(filesPathMap.tailMap("").get(filepath)),
                    R.drawable.ic_launcher_file);
        }
        
        if (selectionMode != SelectionMode.MODE_OPEN_MULTISELECT) {
            SimpleAdapter fileList;
            fileList = new SimpleAdapter(this, mList,
                                         R.layout.file_dialog_row_single,
                                         new String[] { ITEM_KEY, ITEM_IMAGE }, new int[] {
                                             R.id.fdrowtext, R.id.fdrowimage });
            fileList.notifyDataSetChanged();
            setListAdapter(fileList);
        } else {
            ArrayAdapter<HashMap<String, Object>> fileList = new FileDialogArrayAdapter(this,
                                                                                         mList);
            setListAdapter(fileList);
            getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            fileList.notifyDataSetChanged();
            setListAdapter(fileList);
        }
        

    }

    private void addItem(File file, Integer imageId) {
        addItem(file, imageId, file.getName());
    }
    
    private void addItem(File file, Integer imageId, String filelabel) {
        HashMap<String, Object> item = new HashMap<String, Object>();
        item.put(ITEM_KEY, filelabel);
        item.put(ITEM_IMAGE, imageId);
        if (selectionMode == SelectionMode.MODE_OPEN_MULTISELECT) {
            item.put(ITEM_CHECK, selectedPaths.contains(file));
            item.put(ITEM_FILE, file);
        }
        mList.add(item);
    }

    static class ViewHolder {
        protected CheckBox checkbox;
        protected TextView text;
        protected ImageView image;
    }


    public class FileDialogArrayAdapter extends ArrayAdapter<HashMap<String, Object>> {

        private final List<HashMap<String, Object>> list;
        private final Activity context;

        public FileDialogArrayAdapter(Activity context, List<HashMap<String, Object>> list) {
            super(context, R.layout.file_dialog_row_multi, list);
            this.context = context;
            this.list = list;
        }
        
        @Override
            public View getView(int position, View convertView, ViewGroup parent) {
            View view = null;
            if (convertView == null) {
                LayoutInflater inflator = context.getLayoutInflater();
                view = inflator.inflate(R.layout.file_dialog_row_multi, null);
                final ViewHolder viewHolder = new ViewHolder();
                viewHolder.image = (ImageView) view.findViewById(R.id.fdrowimage);
                viewHolder.text = (TextView) view.findViewById(R.id.fdrowtext);
                viewHolder.checkbox = (CheckBox) view.findViewById(R.id.fdrowcheck);
                viewHolder.checkbox
                    .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                            @Override
                                public void onCheckedChanged(CompoundButton buttonView,
                                                             boolean isChecked) {
                                HashMap<String, Object> element =
                                    (HashMap<String, Object>) viewHolder.checkbox
                                    .getTag();
                                element.put(ITEM_CHECK, buttonView.isChecked());
                                if (buttonView.isChecked()) {
                                    selectedPaths.add((File)(element.get(ITEM_FILE)));
                                } else {
                                    selectedPaths.remove((File)(element.get(ITEM_FILE)));
                                }
                            }
                        });
                view.setTag(viewHolder);
                viewHolder.checkbox.setTag(list.get(position));
            } else {
                view = convertView;
                ((ViewHolder) view.getTag()).checkbox.setTag(list.get(position));
            }
            ViewHolder holder = (ViewHolder) view.getTag();
            holder.text.setText((String) list.get(position).get(ITEM_KEY));
            holder.checkbox.setChecked((Boolean) list.get(position).get(ITEM_CHECK));
            holder.image.setImageResource((Integer) list.get(position).get(ITEM_IMAGE));
            return view;
        }
    }
    
    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

        File file = new File(path.get(position));

        setSelectVisible(v);

        if (file.isDirectory()) {
            selectButton.setEnabled(true);
            if (file.canRead()) {
                lastPositions.put(currentPath, position);
                getDir(path.get(position), currentRoot, currentRootName);
            } else {
                new AlertDialog.Builder(this)
                    .setIcon(R.drawable.icon)
                    .setTitle(
                                  "[" + file.getName() + "] "
                                  + getText(R.string.cant_read_folder))
                    .setPositiveButton("OK",
                                       new DialogInterface.OnClickListener() {
                                           
                                           public void onClick(DialogInterface dialog,
                                                               int which) {
                                               
                                           }
                                       }).show();
            }
        } else {
            if (selectionMode == SelectionMode.MODE_OPEN_MULTISELECT) {
            } else {
                selectedFile = file;
                v.setSelected(true);
                selectButton.setEnabled(true);
            }
        }
    }

    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            selectButton.setEnabled(true);

            if (layoutCreate.getVisibility() == View.VISIBLE) {
                layoutCreate.setVisibility(View.GONE);
                layoutSelect.setVisibility(View.VISIBLE);
            } else {
                if (!currentPath.equals(currentRoot)) {
                    getDir(parentPath, currentRoot, currentRootName);
                } else {
                    return super.onKeyDown(keyCode, event);
                }
            }

            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

//    private void setCreateVisible(View v) {
//        layoutCreate.setVisibility(View.VISIBLE);
//        layoutSelect.setVisibility(View.GONE);
//
//        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
//        selectButton.setEnabled(false);
//    }

    private void setSelectVisible(View v) {
        layoutCreate.setVisibility(View.GONE);
        layoutSelect.setVisibility(View.VISIBLE);

        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        selectButton.setEnabled(false);
    }
}