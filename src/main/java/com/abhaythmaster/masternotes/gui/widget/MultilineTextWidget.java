package com.abhaythmaster.masternotes.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.List;

public class MultilineTextWidget extends ClickableWidget {
    private static final int PAD=5,LINE_STEP=11,BG_COLOR=0xAA0A0A1A,BORDER=0x4D7EB8FF,TEXT_COLOR=0xFFE0E0FF,CURSOR_COL=0xFF00D4FF;
    private final TextRenderer tr;
    private final StringBuilder buf;
    private int cursor,scrollLine;

    public MultilineTextWidget(TextRenderer tr,int x,int y,int w,int h){
        super(x,y,w,h,Text.empty());
        this.tr=tr;this.buf=new StringBuilder();
    }
    public String getValue(){return buf.toString();}
    public void setValue(String t){buf.setLength(0);if(t!=null)buf.append(t);cursor=buf.length();scrollLine=0;}
    public void insertText(String s){buf.insert(cursor,s);cursor+=s.length();}

    @Override
    protected void renderWidget(DrawContext ctx,int mx,int my,float delta){
        int x=getX(),y=getY(),w=getWidth(),h=getHeight();
        ctx.fill(x,y,x+w,y+h,BG_COLOR);
        ctx.drawBorder(x,y,w,h,isFocused()?0xFF00D4FF:BORDER);
        ctx.enableScissor(x+1,y+1,x+w-1,y+h-1);
        List<OrderedText> lines=tr.wrapLines(Text.literal(buf.toString()),w-PAD*2);
        int vis=(h-PAD*2)/LINE_STEP;
        scrollLine=Math.min(scrollLine,Math.max(0,lines.size()-vis));
        for(int i=scrollLine;i<Math.min(lines.size(),scrollLine+vis);i++)
            ctx.drawText(tr,lines.get(i),x+PAD,y+PAD+(i-scrollLine)*LINE_STEP,TEXT_COLOR,false);
        if(isFocused()&&(System.currentTimeMillis()/500)%2==0){
            int rem=cursor,li=0;
            for(int i=0;i<lines.size();i++){
                StringBuilder sb=new StringBuilder();
                lines.get(i).accept((idx,st,ch)->{sb.appendCodePoint(ch);return true;});
                if(rem<=sb.length()){li=i;break;}
                rem-=sb.length();li=i+1;
            }
            if(li>=scrollLine&&li<scrollLine+vis)
                ctx.fill(x+PAD+rem,y+PAD+(li-scrollLine)*LINE_STEP,x+PAD+rem+1,y+PAD+(li-scrollLine)*LINE_STEP+LINE_STEP-1,CURSOR_COL);
        }
        ctx.disableScissor();
    }

    @Override
    public boolean keyPressed(int key,int scan,int mods){
        if(!isFocused())return false;
        boolean ctrl=(mods&GLFW.GLFW_MOD_CONTROL)!=0;
        switch(key){
            case GLFW.GLFW_KEY_BACKSPACE:if(ctrl){int i=cursor-1;while(i>0&&buf.charAt(i-1)!=' '&&buf.charAt(i-1)!='\n')i--;buf.delete(i,cursor);cursor=i;}else if(cursor>0)buf.deleteCharAt(--cursor);return true;
            case GLFW.GLFW_KEY_DELETE:if(cursor<buf.length())buf.deleteCharAt(cursor);return true;
            case GLFW.GLFW_KEY_LEFT:if(cursor>0)cursor--;return true;
            case GLFW.GLFW_KEY_RIGHT:if(cursor<buf.length())cursor++;return true;
            case GLFW.GLFW_KEY_HOME:cursor=0;scrollLine=0;return true;
            case GLFW.GLFW_KEY_END:cursor=buf.length();return true;
            case GLFW.GLFW_KEY_UP:scrollLine=Math.max(0,scrollLine-1);return true;
            case GLFW.GLFW_KEY_DOWN:scrollLine++;return true;
            case GLFW.GLFW_KEY_ENTER:case GLFW.GLFW_KEY_KP_ENTER:buf.insert(cursor++,'\n');return true;
        }
        if(ctrl){
            if(key==GLFW.GLFW_KEY_A){cursor=buf.length();return true;}
            if(key==GLFW.GLFW_KEY_C){MinecraftClient.getInstance().keyboard.setClipboard(buf.toString());return true;}
            if(key==GLFW.GLFW_KEY_V){String c=MinecraftClient.getInstance().keyboard.getClipboard();buf.insert(cursor,c);cursor+=c.length();return true;}
            if(key==GLFW.GLFW_KEY_X){MinecraftClient.getInstance().keyboard.setClipboard(buf.toString());buf.setLength(0);cursor=0;return true;}
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr,int mods){if(!isFocused())return false;if(chr>=32){buf.insert(cursor++,chr);return true;}return false;}

    @Override
    public boolean mouseScrolled(double mx,double my,double h,double v){if(isMouseOver(mx,my)){scrollLine=Math.max(0,scrollLine-(int)v);return true;}return false;}

    @Override
    public boolean mouseClicked(double mx,double my,int btn){if(isMouseOver(mx,my)){setFocused(true);return true;}setFocused(false);return false;}

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder b){}
}
