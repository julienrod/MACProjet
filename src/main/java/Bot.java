//Sources : https://monsterdeveloper.gitbooks.io/writing-telegram-bots-on-java/content/

import org.bson.types.ObjectId;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.StrictMath.toIntExact;

public class Bot extends TelegramLongPollingBot {
    private HashMap<Long, Integer> addReceipeStatus = new HashMap<>();
    private HashMap<Long, List<List<String>>> addReceipeData = new HashMap<>();

    public void onUpdateReceived(Update update) {
        long user_id = update.getMessage( ).getChat( ).getId( );
        if(update.hasMessage() && update.getMessage().hasText()) {
            String user_first_name = update.getMessage( ).getChat( ).getFirstName( );
            String user_last_name = update.getMessage( ).getChat( ).getLastName( );
            String user_username = update.getMessage( ).getChat( ).getUserName( );
            String message_text = update.getMessage( ).getText( );
            MongoDBDAO.getInstance( ).check(user_first_name, user_last_name, toIntExact(user_id), user_username);
            long chat_id = update.getMessage( ).getChatId( );
            SendMessage message = null;
            if(addReceipeStatus.containsKey(user_id)){
                switch (addReceipeStatus.get(user_id)){
                    case 0:
                        newReceipeList(user_id, message_text);
                        message = new SendMessage( ).setChatId(chat_id).setText("Veuillez spécifier les ustenciles (" +
                                "séparés par une virgule)\n Exemple: mixer, micro-onde, spatule");
                        break;
                    case 1:
                        newReceipeList(user_id, message_text);
                        message = new SendMessage( ).setChatId(chat_id).setText("Veuillez spécifier le temps de" +
                                " préparation\n Exemple: 1h 30m");
                        break;
                    case 2:
                        if(!newReceipeRegex(user_id, message_text, "([0-9]+m|[0-9]+h|[0-9]+h [0-9]+m)")){
                            message = new SendMessage( ).setChatId(chat_id).setText("Pattern de temps incorrecte");
                        }else{
                            message = new SendMessage( ).setChatId(chat_id).setText("Veuillez spécifier le nombre de" +
                                    " calories\n Exemple: 250kcal");
                        }
                        break;
                    case 3:
                        if(!newReceipeRegex(user_id, message_text, "([0-9]+(kcal)?)")){
                            message = new SendMessage( ).setChatId(chat_id).setText("Pattern de temps incorrecte");
                        }else{
                            message = new SendMessage( ).setChatId(chat_id).setText("Veuillez dérire la recette");
                        }
                        break;
                    case 4:
                        newReceipeSinglePhrase(user_id, message_text);
                        message = new SendMessage( ).setChatId(chat_id).setText("Vous pouvez aussi spécifier d'autres " +
                                "tags\n Exemples : biscuit, gateau, pâtisserie japonaise, etc...");
                        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                        List<InlineKeyboardButton> rowInline = new ArrayList<>();
                        rowInline.add(new InlineKeyboardButton().setText("Non merci").setCallbackData("nothankyou"));
                        rowsInline.add(rowInline);
                        markupInline.setKeyboard(rowsInline);
                        message.setReplyMarkup(markupInline);
                        break;
                    case 5:
                        newReceipeList(user_id, message_text);
                        addNewReceipe(user_id);
                        message = new SendMessage( ).setChatId(chat_id).setText("La recette a été ajoutée");
                        break;
                }

            }else if(message_text.equals("/markup")){
                message = new SendMessage( ).setChatId(chat_id).setText("Here is your keyboard");
                ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                List<KeyboardRow> keyboard = new ArrayList<>();
                KeyboardRow row = new KeyboardRow();
                row.add("Row 1 Button 1");
                row.add("Row 1 Button 2");
                row.add("Row 1 Button 3");
                keyboard.add(row);
                row = new KeyboardRow();
                row.add("Row 2 Button 1");
                row.add("Row 2 Button 2");
                row.add("Row 2 Button 3");
                keyboard.add(row);
                keyboardMarkup.setKeyboard(keyboard);
                message.setReplyMarkup(keyboardMarkup);
            }else if (message_text.equals("/hide")) {
                message = new SendMessage()
                        .setChatId(chat_id)
                        .setText("Keyboard hidden");
                ReplyKeyboardRemove keyboardMarkup = new ReplyKeyboardRemove();
                message.setReplyMarkup(keyboardMarkup);
                try {
                    execute(message); // Call method to send the photo
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }else if (message_text.equals("/neo4j")) {
                try ( Neo4jDAO greeter = Neo4jDAO.getInstance() )
                {
                    //greeter.addMember( "hello, world" );
                }catch(Exception e){
                    e.printStackTrace();
                }
            }else if (message_text.startsWith("/newreceipe ")) {
                newReceipeSinglePhrase(user_id, message_text.substring(12));
                message = new SendMessage( ).setChatId(chat_id).setText("Veuillez spécifier les ingrédients (" +
                        "séparés par une virgule) et leurs quantités" +
                        "(séparés les quantités des ingrédients avec un slash" +
                        ")\n Exemple: sucre/250g, farine/150g, eau/2 tasse, confiture d'abricot/1 pot");
            }else if (message_text.equals("/reset")) {
                addReceipeData.remove(user_id);
                addReceipeStatus.remove(user_id);
                message = new SendMessage( ).setChatId(chat_id).setText("L'ajout de recette a été avorté");
            }else if (message_text.equals("/random")) {
            }else if (message_text.startsWith("/receipesbyname ")) {
            }else if (message_text.startsWith("/receipesbyingredients ")) {
                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                rowInline.add(new InlineKeyboardButton().setText("Like these ingredients").setCallbackData("update_msg_text"));
                rowsInline.add(rowInline);
                markupInline.setKeyboard(rowsInline);
                message.setReplyMarkup(markupInline);
            }else if (message_text.startsWith("/receipesbyuser ")) {
                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                rowInline.add(new InlineKeyboardButton().setText("Like this user").setCallbackData("update_msg_text"));
                rowsInline.add(rowInline);
                markupInline.setKeyboard(rowsInline);
                message.setReplyMarkup(markupInline);
            }else if (message_text.startsWith("/receipesbycalory ")) {
            }else if (message_text.startsWith("/receipesbymachine ")) {
                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                rowInline.add(new InlineKeyboardButton().setText("Like these machines").setCallbackData("update_msg_text"));
                rowsInline.add(rowInline);
                markupInline.setKeyboard(rowsInline);
                message.setReplyMarkup(markupInline);
            }else if (message_text.startsWith("/receipesbytime ")) {
            }else if (message_text.startsWith("/showreceipe ")) {
                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                rowInline.add(new InlineKeyboardButton().setText("Like this receipe").setCallbackData("update_msg_text"));
                rowsInline.add(rowInline);
                markupInline.setKeyboard(rowsInline);
                message.setReplyMarkup(markupInline);
            }else if (message_text.equals("/userscooking")) {
            }else if (message_text.startsWith("/recommandations ")) {
            }else if (message_text.equals("/help")) {
            }else{
                message = new SendMessage( ).setChatId(chat_id).setText(message_text);
                InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                rowInline.add(new InlineKeyboardButton().setText("Update message text").setCallbackData("update_msg_text"));
                rowsInline.add(rowInline);
                markupInline.setKeyboard(rowsInline);
                message.setReplyMarkup(markupInline);
            }
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }else if (update.hasCallbackQuery()) {
            //A utiliser pour rajouter des like
            String call_data = update.getCallbackQuery().getData();
            long message_id = update.getCallbackQuery().getMessage().getMessageId();
            long chat_id = update.getCallbackQuery().getMessage().getChatId();
            if(call_data.equals("nothankyou")){
                addNewReceipe(user_id);
            }else if (call_data.equals("update_msg_text")) {
                String answer = "Updated message text";
                EditMessageText new_message = new EditMessageText()
                        .setChatId(chat_id)
                        .setMessageId((int)message_id)
                        .setText(answer);
                try {
                    execute(new_message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getBotUsername() {
        return "SyugarDaddyBot";
    }

    public String getBotToken() {
        return "1057276327:AAHJLLnJ-4kCdQWe5hCnFtJB5V8ZGf7FTwY";
    }

    private void newReceipeSinglePhrase(long id, String receipeName){
        int state = addReceipeStatus.get(id) == null ? -1 : addReceipeStatus.get(id);
        List<List<String>> newReceipe = new ArrayList<>();
        List<String> name = new ArrayList<>();
        name.add(receipeName);
        if(addReceipeStatus.get(id) != null){
            newReceipe = addReceipeData.get(id);
        }
        newReceipe.add(name);
        addReceipeData.put(id, newReceipe);
        addReceipeStatus.put(id, ++state);

    }

    private void newReceipeList(long id, String list){
        int state = addReceipeStatus.get(id);
        List<String> myList = Arrays.asList(list.replace("[^a-zA-Z/]+", "")
                .toLowerCase().replace(" ", "").split(","));
        List<List<String>> newReceipe = addReceipeData.get(id);
        newReceipe.add(myList);
        addReceipeData.put(id, newReceipe);
        addReceipeStatus.put(id, ++state);
    }

    private boolean newReceipeRegex(long id, String list, String regex){
        int state = addReceipeStatus.get(id);
        Pattern pat = Pattern.compile(regex);
        Matcher match = pat.matcher(list);
        if(!match.find()) return false;
        List<String> myList = Arrays.asList(list.toLowerCase());
        List<List<String>> newReceipe = addReceipeData.get(id);
        newReceipe.add(myList);
        addReceipeData.put(id, newReceipe);
        addReceipeStatus.put(id, ++state);
        return true;
    }

    private void addNewReceipe(long id){
        //Nom de la recette, ingrédients, ustenciles, temps, kcal, description (, sous catégorie)
        String name = addReceipeData.get(id).get(0).get(0);
        String description = addReceipeData.get(id).get(5).get(0);
        String time = addReceipeData.get(id).get(3).get(0);
        String kcal = addReceipeData.get(id).get(4).get(0);
        List<String> ingredients = addReceipeData.get(id).get(1);
        List<String> ustenciles = addReceipeData.get(id).get(2);
        List<String> subcategories = null;
        if (addReceipeData.get(id).size() > 6){
            subcategories = addReceipeData.get(id).get(6);
        }
        ObjectId receipeId = MongoDBDAO.getInstance().addreceipe(name, description, time, kcal);
        Neo4jDAO.getInstance().addReceipe(id, receipeId.toString(), ingredients, ustenciles, subcategories);
    }
}
